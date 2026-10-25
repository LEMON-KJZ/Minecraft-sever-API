# 发布到 GitHub

GitHub 官方建议：新建空仓库后，把本地项目添加 remote，再推送 `main` 分支；创建仓库时不要同时勾选 README、License 或 Git ignore，避免第一次推送产生冲突。

## 方式一：GitHub 网页 + Git 命令

1. 打开 https://github.com/new。
2. 仓库名可以填写 `minecraft-server-api-fabric`。
3. 选择 Public 或 Private。
4. 不要勾选 README、License、`.gitignore`，然后创建仓库。
5. 在本地项目目录打开 PowerShell 或 Git Bash：

```powershell
cd C:\Users\klgkj\Documents\Codex\2026-10-24\minecraft-java-26-3-fabric-app

git init -b main
git add .
git commit -m "Initial Minecraft 26.3 Fabric server API"
git remote add origin https://github.com/YOUR_ACCOUNT/minecraft-server-api-fabric.git
git push -u origin main
```

如果项目已经初始化过 Git，不要再次执行 `git init`，直接执行：

```powershell
git add .
git commit -m "Update server API mod"
git remote add origin https://github.com/YOUR_ACCOUNT/minecraft-server-api-fabric.git
git push -u origin main
```

如果提示 `remote origin already exists`：

```powershell
git remote set-url origin https://github.com/YOUR_ACCOUNT/minecraft-server-api-fabric.git
git push -u origin main
```

GitHub 登录可以使用 GitHub CLI、Git Credential Manager 或 Personal Access Token；不要把密码写进 remote URL。

## 方式二：GitHub CLI

安装并登录 `gh` 后，在项目目录执行：

```powershell
gh auth login
gh repo create YOUR_ACCOUNT/minecraft-server-api-fabric --source=. --public --remote=origin --push
```

把 `--public` 改成 `--private` 可以创建私有仓库。

## 发布前检查

```powershell
git status
git check-ignore -v build work outputs config/serverapi.json
```

不要提交真实服务器生成的 `config/serverapi.json`，其中包含 API Key。仓库只提交 `config/serverapi.json.example`。GitHub 官方也明确提醒不要提交密码或 API Key。[GitHub：将本地代码添加到 GitHub](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)

## 发布 JAR

构建后把以下文件作为 GitHub Release 附件上传：

- `outputs/serverapi-fabric-26.3-1.0.0.jar`
- `outputs/serverapi-fabric-26.3-1.0.0-sources.zip`

仓库代码用于构建，Release JAR 用于服务器安装。
