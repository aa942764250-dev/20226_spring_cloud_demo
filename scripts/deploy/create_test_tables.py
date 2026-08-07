import paramiko
import tempfile
import os

SERVER = "154.201.68.122"
USER = "root"
PASS = "keS79Ko6a9hP"

SQL_FILE = r"D:\Workspace\springcloud-demo\sql\init.sql"

print(f"连接 {SERVER}...")
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(SERVER, username=USER, password=PASS, timeout=15)
print("连接成功")

sftp = ssh.open_sftp()
remote_sql = "/tmp/init_tables.sql"
print(f"上传 SQL 文件到 {remote_sql}...")
sftp.put(SQL_FILE, remote_sql)
sftp.close()
print("上传完成")

print("\n=== 执行建表SQL ===")
cmd = f"mysql -u app_user -p'App@Remote2026#User' < {remote_sql}"
stdin, stdout, stderr = ssh.exec_command(cmd, timeout=30)
out = stdout.read().decode("utf-8", errors="replace")
err = stderr.read().decode("utf-8", errors="replace")
exit_code = stdout.channel.recv_exit_status()
if out.strip():
    print(out.rstrip())
if err.strip():
    print(f"[stderr] {err.rstrip()}")
print(f"exit code: {exit_code}")

print("\n=== 验证表 ===")
stdin, stdout, stderr = ssh.exec_command("mysql -u app_user -p'App@Remote2026#User' springcloud_demo -e 'SHOW TABLES;'", timeout=10)
out = stdout.read().decode("utf-8", errors="replace")
err = stderr.read().decode("utf-8", errors="replace")
if out.strip():
    print(out.rstrip())
if err.strip():
    print(f"[stderr] {err.rstrip()}")

print("\n=== 验证数据量 ===")
for t in ["user", "customer", "employee", "orders", "contract"]:
    stdin, stdout, stderr = ssh.exec_command(f"mysql -u app_user -p'App@Remote2026#User' springcloud_demo -e 'SELECT COUNT(*) AS cnt FROM {t};'", timeout=10)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    print(f"{t}: {out.strip()}")
    if err.strip():
        print(f"  [stderr] {err.rstrip()}")

ssh.close()
print("\n完成")
