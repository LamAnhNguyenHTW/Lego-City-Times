# Lego-City-Times

## Start (mit Alertmanager-Render)

Windows PowerShell:

```powershell
./scripts/up.ps1 -Build
```

Linux/macOS:

```sh
./scripts/up.sh --build
```

```
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

uv tool install specify-cli --from git+https://github.com/github/spec-kit.git@v0.8.10


specify init my-project --integration copilot