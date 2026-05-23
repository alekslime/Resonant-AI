const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");
const { spawn } = require("child_process");
const fs = require("fs");

const isDev = !app.isPackaged;
const isWindows = process.platform === "win32";

// Where we store the user's .env.local
const envPath = path.join(app.getPath("userData"), ".env.local");

// Where the PyInstaller binaries live
function getBinaryPath(name) {
  const ext = isWindows ? ".exe" : "";
  if (isDev) {
    return path.join(__dirname, "resources", `${name}${ext}`);
  }
  return path.join(process.resourcesPath, `${name}${ext}`);
}

let serverProcess = null;
let agentProcess = null;
let mainWindow = null;

function readEnv() {
  if (!fs.existsSync(envPath)) return {};
  const lines = fs.readFileSync(envPath, "utf8").split("\n");
  const env = {};
  for (const line of lines) {
    const [key, ...rest] = line.split("=");
    if (key && rest.length) env[key.trim()] = rest.join("=").trim();
  }
  return env;
}

function writeEnv(vars) {
  const content = Object.entries(vars)
    .map(([k, v]) => `${k}=${v}`)
    .join("\n");
  fs.writeFileSync(envPath, content, "utf8");
}

function hasValidEnv() {
  const env = readEnv();
  return env.LIVEKIT_URL && env.LIVEKIT_API_KEY && env.LIVEKIT_API_SECRET;
}

function startPythonProcesses() {
  const env = { ...process.env, ...readEnv() };

  const serverBin = getBinaryPath("server");
  const agentBin = getBinaryPath("agent");

  serverProcess = spawn(serverBin, [], { env, stdio: "pipe" });
  serverProcess.stdout.on("data", (d) => console.log("[server]", d.toString()));
  serverProcess.stderr.on("data", (d) => console.error("[server]", d.toString()));

  // Give server a second to bind port 5000 before starting agent
  setTimeout(() => {
    agentProcess = spawn(agentBin, ["dev"], { env, stdio: "pipe" });
    agentProcess.stdout.on("data", (d) => console.log("[agent]", d.toString()));
    agentProcess.stderr.on("data", (d) => console.error("[agent]", d.toString()));
  }, 1500);
}

function stopPythonProcesses() {
  if (serverProcess) { serverProcess.kill(); serverProcess = null; }
  if (agentProcess) { agentProcess.kill(); agentProcess = null; }
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 750,
    minWidth: 800,
    minHeight: 600,
    title: "Resonant",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadFile("index.html");
  mainWindow.on("closed", () => { mainWindow = null; });
}

function createSetupWindow() {
  const setupWindow = new BrowserWindow({
    width: 520,
    height: 480,
    resizable: false,
    title: "Resonant — Setup",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  setupWindow.loadFile("setup.html");

  ipcMain.once("save-env", (_event, vars) => {
    writeEnv(vars);
    setupWindow.close();
    startPythonProcesses();
    // Small delay so server is ready before the UI tries to hit it
    setTimeout(createMainWindow, 2000);
  });
}

app.whenReady().then(() => {
  if (hasValidEnv()) {
    startPythonProcesses();
    setTimeout(createMainWindow, 2000);
  } else {
    createSetupWindow();
  }
});

app.on("window-all-closed", () => {
  stopPythonProcesses();
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", stopPythonProcesses);
