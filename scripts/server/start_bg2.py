import subprocess, os, sys

os.chdir(r"D:\Workspace\springcloud-demo")

log_out = open(r"D:\Workspace\springcloud-demo\boot-out2.log", "w")

proc = subprocess.Popen(
    ["mvn.cmd", "spring-boot:run", "-pl", "springcloud-service", "-Dspring-boot.run.profiles=dev"],
    stdout=log_out,
    stderr=subprocess.STDOUT,
    shell=True,
    cwd=r"D:\Workspace\springcloud-demo"
)

print(f"Spring Boot starting in background, PID={proc.pid}, check boot-out2.log")
sys.stdout.flush()