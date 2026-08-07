import subprocess, sys, os
os.chdir(r"D:\Workspace\springcloud-demo")
proc = subprocess.Popen(
    ["mvn", "spring-boot:run", "-pl", "springcloud-service", "-Dspring-boot.run.profiles=dev"],
    stdout=open(r"D:\Workspace\springcloud-demo\boot-out.log", "w"),
    stderr=open(r"D:\Workspace\springcloud-demo\boot-err.log", "w"),
    shell=True
)
print(f"PID={proc.pid}")
proc.wait()