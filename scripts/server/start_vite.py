import subprocess, os
os.chdir(r"D:\Workspace\Project_003_QueryFrontend")
subprocess.Popen(
    ["npx.cmd", "vite", "--host", "0.0.0.0"],
    stdout=open(r"D:\Workspace\Project_003_QueryFrontend\vite-out.log", "w"),
    stderr=subprocess.STDOUT,
    shell=False
)
print("Vite starting in background")