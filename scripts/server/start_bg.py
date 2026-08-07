import subprocess, os
os.chdir(r"D:\Workspace\springcloud-demo")
subprocess.Popen(
    ["mvn.cmd", "spring-boot:run", "-pl", "springcloud-service", "-Dspring-boot.run.profiles=dev"],
    stdout=open(r"D:\Workspace\springcloud-demo\boot-out.log", "w"),
    stderr=subprocess.STDOUT,
    shell=False
)
print("Spring Boot starting in background, check boot-out.log")