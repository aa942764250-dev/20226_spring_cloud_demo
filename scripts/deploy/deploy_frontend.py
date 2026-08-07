import paramiko
import os
import sys

SERVER = "154.201.68.122"
USER = "root"
PASS = "keS79Ko6a9hP"
REMOTE_DIR = "/opt/apps/docker/frontend"
LOCAL_DIST = r"D:\Workspace\Project_003_QueryFrontend\dist"
LOCAL_NGINX = r"D:\Workspace\Project_003_QueryFrontend\deploy\nginx.conf"
LOCAL_DOCKERFILE = r"D:\Workspace\Project_003_QueryFrontend\deploy\Dockerfile"


def sftp_upload_dir(sftp, local_dir, remote_dir):
    """递归上传本地目录到远程"""
    try:
        sftp.stat(remote_dir)
    except FileNotFoundError:
        sftp.mkdir(remote_dir)
    for item in os.listdir(local_dir):
        local_path = os.path.join(local_dir, item)
        remote_path = remote_dir + "/" + item
        if os.path.isdir(local_path):
            sftp_upload_dir(sftp, local_path, remote_path)
        else:
            print(f"  上传: {item} ({os.path.getsize(local_path)/1024:.1f} KB)")
            sftp.put(local_path, remote_path)


def run_cmd(ssh, cmd, timeout=300):
    print(f">>> {cmd}")
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    exit_code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.rstrip())
    if err.strip() and "WARNING" not in err:
        print(f"[stderr] {err.rstrip()}")
    return exit_code


print(f"连接 {SERVER}...")
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(SERVER, username=USER, password=PASS, timeout=15)
print("连接成功")

# 创建远程目录
run_cmd(ssh, f"mkdir -p {REMOTE_DIR}")

# 上传文件
print("\n=== 上传文件 ===")
sftp = ssh.open_sftp()
print("上传 dist/...")
sftp_upload_dir(sftp, LOCAL_DIST, REMOTE_DIR + "/dist")
print("上传 nginx.conf...")
sftp.put(LOCAL_NGINX, REMOTE_DIR + "/nginx.conf")
print("上传 Dockerfile...")
sftp.put(LOCAL_DOCKERFILE, REMOTE_DIR + "/Dockerfile")
sftp.close()
print("上传完成")

# 构建并启动容器
print("\n=== 构建并启动容器 ===")
run_cmd(ssh, "docker stop query-frontend 2>/dev/null || true")
run_cmd(ssh, "docker rm query-frontend 2>/dev/null || true")
run_cmd(ssh, f"cd {REMOTE_DIR} && docker build -t query-frontend .", timeout=300)
run_cmd(ssh, "docker run -d --name query-frontend --restart unless-stopped --network app-net -p 5180:80 query-frontend")

# 验证
print("\n=== 验证 ===")
import time
time.sleep(3)
run_cmd(ssh, 'docker ps --filter name=query-frontend --format "{{.Names}} {{.Status}} {{.Ports}}"')
run_cmd(ssh, 'curl -s -o /dev/null -w "前端 HTTP %{http_code}\\n" http://localhost:5180/')
run_cmd(ssh, '''curl -s -w "\\nAPI HTTP %{http_code}\\n" http://localhost:5180/api/query -X POST -H "Content-Type: application/json" -d '{"sql":"SELECT 1","limit":10}' ''')

print("\n部署完成！访问: http://154.201.68.122:5180/")
ssh.close()
