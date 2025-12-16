# Project Documentation

This directory contains general documentation for EBS projects located in `ScriptInterpreter/projects/`.

## Purpose

Use this directory for:
- Cross-project documentation
- Design specifications shared across multiple projects
- Architecture documents
- Best practices and guidelines
- Tutorials and how-to guides

## Project-Specific Documentation

For documentation specific to a single project, place it in the project's own directory:
- `ScriptInterpreter/projects/my-project/README.md`
- `ScriptInterpreter/projects/my-project/DESIGN.md`
- etc.

## Directory Structure

```
ScriptInterpreter/projects/
├── doc/                    # General/shared documentation (this directory)
│   ├── README.md
│   └── [other shared docs]
├── project1/               # Individual project directories
│   ├── project.json
│   ├── main.ebs
│   └── README.md
├── project2/
│   └── ...
└── ...
```
