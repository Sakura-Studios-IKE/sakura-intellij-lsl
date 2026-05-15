package com.sakurastudios.lsl

import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * "New Project → Sakura LSL" project type.
 *
 * Creates one of two starter project layouts:
 *
 *   Just LSL          scripts/  tests/  README.md  .gitignore
 *   Full Stack LSL    scripts/  tests/  backend/   frontend/  migrations/
 *                     docker-compose.yml  Makefile  README.md  .gitignore
 *
 * The "Full Stack" template is intentionally light — it generates the
 * directory skeleton and starter files so the user has a coherent
 * place to start. IntelliJ Ultimate already handles Python, JavaScript,
 * React, SQL, and Docker editing natively; this template just lays the
 * scaffold out the way Sakura's house style expects.
 */
class JustLslProjectGenerator : DirectoryProjectGeneratorBase<Any>() {
    override fun getName(): String = "Sakura LSL — Just LSL"
    override fun getLogo(): Icon? = LslIcons.FILE
    override fun generateProject(project: Project, baseDir: VirtualFile, settings: Any, module: com.intellij.openapi.module.Module) {
        scaffoldJustLsl(baseDir)
        refresh(baseDir)
    }
}

class FullStackLslProjectGenerator : DirectoryProjectGeneratorBase<Any>() {
    override fun getName(): String = "Sakura LSL — Full Stack (LSL + FastAPI + Alembic + React)"
    override fun getLogo(): Icon? = LslIcons.FILE
    override fun generateProject(project: Project, baseDir: VirtualFile, settings: Any, module: com.intellij.openapi.module.Module) {
        scaffoldJustLsl(baseDir)
        scaffoldFullStack(baseDir)
        refresh(baseDir)
    }
}

private fun refresh(dir: VirtualFile) {
    LocalFileSystem.getInstance().refreshAndFindFileByPath(dir.path)?.refresh(false, true)
}

private fun put(parent: VirtualFile, path: String, body: String) {
    val abs = parent.path + "/" + path
    FileUtil.writeToFile(java.io.File(abs), body)
}

private fun scaffoldJustLsl(dir: VirtualFile) {
    val name = dir.name
    put(dir, "scripts/hello.lsl", """
        |// Replace this with your LSL. Compile via right-click → "Run in slemu".
        |default {
        |    state_entry() {
        |        llOwnerSay("hello from ${name}");
        |    }
        |}
        |""".trimMargin())

    put(dir, "tests/test_hello.py", """
        |"\"\"Quick sanity test driven by sakura-lsltest. Run with `make test`.\"\"\"
        |from pathlib import Path
        |import lsltest
        |
        |SCRIPTS = Path(__file__).parent.parent / "scripts"
        |
        |@lsltest.compile_ok(SCRIPTS / "hello.lsl")
        |def test_hello_compiles():
        |    pass
        |""".trimMargin())

    put(dir, "README.md", """
        |# ${name}
        |
        |A Sakura LSL project. Build, run, and test entirely from your IDE
        |with the [Sakura LSL plugin](https://github.com/Sakura-Studios-IKE/sakura-intellij-lsl).
        |
        |## Quick start
        |
        |- Edit any `.lsl` under `scripts/`.
        |- Right-click → **Run in slemu (current file)** to execute it locally.
        |- `make test` runs every Python test in `tests/`.
        |
        |## Layout
        |
        |    scripts/   your LSL sources
        |    tests/     sakura-lsltest scenarios (pytest-style)
        |
        |## Toolchain
        |
        |- [sakura-lslc](https://github.com/Sakura-Studios-IKE/sakura-lslc) — compiler
        |- [sakura-slemu](https://github.com/Sakura-Studios-IKE/sakura-slemu) — emulator
        |- [sakura-lsldb](https://github.com/Sakura-Studios-IKE/sakura-lsldb) — debugger
        |- [sakura-lsltest](https://github.com/Sakura-Studios-IKE/sakura-lsltest) — tests
        |""".trimMargin())

    put(dir, "Makefile", """
        |.PHONY: test compile
        |
        |compile:
        |	@for f in scripts/*.lsl; do lslc -c "${'$'}${'$'}f"; done
        |
        |test:
        |	@python3 -m lsltest run tests/
        |""".trimMargin())

    put(dir, ".gitignore", """
        |scripts/*.lslbc
        |slemu_volume/
        |__pycache__/
        |.venv/
        |node_modules/
        |build/
        |dist/
        |.idea/
        |""".trimMargin())
}

private fun scaffoldFullStack(dir: VirtualFile) {
    val name = dir.name

    // FastAPI backend
    put(dir, "backend/pyproject.toml", """
        |[project]
        |name = "${name}-backend"
        |version = "0.1.0"
        |requires-python = ">=3.10"
        |dependencies = [
        |    "fastapi>=0.110",
        |    "uvicorn[standard]>=0.27",
        |    "sqlalchemy>=2.0",
        |    "alembic>=1.13",
        |    "psycopg[binary]>=3.1",
        |    "pydantic>=2.6",
        |]
        |""".trimMargin())

    put(dir, "backend/app/__init__.py", "")
    put(dir, "backend/app/main.py", """
        |from fastapi import FastAPI
        |
        |app = FastAPI(title="${name}")
        |
        |@app.get("/health")
        |def health() -> dict:
        |    return {"ok": True, "service": "${name}"}
        |
        |# Wire LSL-script callbacks here. Example endpoints your prims hit
        |# via llHTTPRequest typically include /auth/pair, /assets/register,
        |# /accounting/ledger. Add them as you grow.
        |""".trimMargin())

    put(dir, "backend/app/db.py", """
        |from sqlalchemy.orm import declarative_base, sessionmaker
        |from sqlalchemy import create_engine
        |import os
        |
        |DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+psycopg://localhost/${name}")
        |engine = create_engine(DATABASE_URL, future=True)
        |SessionLocal = sessionmaker(bind=engine, expire_on_commit=False, autoflush=False)
        |Base = declarative_base()
        |""".trimMargin())

    // Alembic
    put(dir, "backend/alembic.ini", """
        |[alembic]
        |script_location = migrations
        |sqlalchemy.url = postgresql+psycopg://localhost/${name}
        |""".trimMargin())
    put(dir, "backend/migrations/env.py", """
        |from logging.config import fileConfig
        |from alembic import context
        |from sqlalchemy import engine_from_config, pool
        |from app.db import Base
        |
        |config = context.config
        |if config.config_file_name:
        |    fileConfig(config.config_file_name)
        |target_metadata = Base.metadata
        |
        |def run_migrations_offline() -> None:
        |    context.configure(url=config.get_main_option("sqlalchemy.url"), target_metadata=target_metadata, literal_binds=True)
        |    with context.begin_transaction():
        |        context.run_migrations()
        |
        |def run_migrations_online() -> None:
        |    connectable = engine_from_config(config.get_section(config.config_ini_section), prefix="sqlalchemy.", poolclass=pool.NullPool)
        |    with connectable.connect() as connection:
        |        context.configure(connection=connection, target_metadata=target_metadata)
        |        with context.begin_transaction():
        |            context.run_migrations()
        |
        |if context.is_offline_mode():
        |    run_migrations_offline()
        |else:
        |    run_migrations_online()
        |""".trimMargin())
    // Alembic's script.py.mako template is generated by `alembic init` —
    // we don't ship a copy here because its Mako $${...} syntax clashes
    // with Kotlin string interpolation. The user runs `alembic init`
    // once after creating the project.

    // React frontend (Vite-style starter)
    put(dir, "frontend/package.json", """
        |{
        |  "name": "${name}-frontend",
        |  "private": true,
        |  "version": "0.0.1",
        |  "type": "module",
        |  "scripts": {
        |    "dev": "vite",
        |    "build": "vite build",
        |    "preview": "vite preview"
        |  },
        |  "dependencies": {
        |    "react": "^18.3.0",
        |    "react-dom": "^18.3.0"
        |  },
        |  "devDependencies": {
        |    "vite": "^5.2.0",
        |    "@vitejs/plugin-react": "^4.3.0"
        |  }
        |}
        |""".trimMargin())
    put(dir, "frontend/index.html", """
        |<!doctype html>
        |<html lang="en">
        |<head>
        |    <meta charset="UTF-8"/>
        |    <title>${name}</title>
        |</head>
        |<body>
        |    <div id="root"></div>
        |    <script type="module" src="/src/main.jsx"></script>
        |</body>
        |</html>
        |""".trimMargin())
    put(dir, "frontend/src/main.jsx", """
        |import React from "react";
        |import { createRoot } from "react-dom/client";
        |
        |function App() {
        |    return <h1>${name}</h1>;
        |}
        |createRoot(document.getElementById("root")).render(<App/>);
        |""".trimMargin())

    // Compose + Makefile additions
    put(dir, "docker-compose.yml", """
        |services:
        |  db:
        |    image: postgres:16
        |    environment:
        |      POSTGRES_DB: ${name}
        |      POSTGRES_USER: ${name}
        |      POSTGRES_PASSWORD: dev
        |    ports: ["5432:5432"]
        |    volumes: ["./.pgdata:/var/lib/postgresql/data"]
        |
        |  backend:
        |    build: ./backend
        |    depends_on: [db]
        |    ports: ["8000:8000"]
        |    environment:
        |      DATABASE_URL: postgresql+psycopg://${name}:dev@db/${name}
        |    command: uvicorn app.main:app --host 0.0.0.0 --port 8000
        |""".trimMargin())

    // Overwrite Makefile with full-stack targets
    put(dir, "Makefile", """
        |.PHONY: dev test compile migrate frontend up down
        |
        |dev: up
        |	@cd backend && uvicorn app.main:app --reload
        |
        |up:
        |	docker compose up -d db
        |
        |down:
        |	docker compose down
        |
        |migrate:
        |	@cd backend && alembic upgrade head
        |
        |frontend:
        |	@cd frontend && npm run dev
        |
        |compile:
        |	@for f in scripts/*.lsl; do lslc -c "${'$'}${'$'}f"; done
        |
        |test:
        |	@python3 -m lsltest run tests/
        |""".trimMargin())
}
