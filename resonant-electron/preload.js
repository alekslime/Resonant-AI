const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("resonant", {
  saveEnv: (vars) => ipcRenderer.send("save-env", vars),
});
