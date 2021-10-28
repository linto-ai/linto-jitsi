# Require
Require to work with the [linto-stack](https://github.com/linto-ai/linto-platform-stack)

# Configuration

This repo provides a tool that *tries* to solve all the burden of  deploying LinTO's server components with our proposed Docker images (quite a complicated task otherwise...)

The tool is available here, [linto-platform-stack](https://github.com/linto-ai/linto-platform-stack). It mainly consists of a bash script, `start.sh`, that feeds Docker Swarm with the provided YML Docker Compose files. The script will also generate files in a shared folder made available on every node of the swarm cluster. Almost every user setups are wrapped in a single environement variable declarative file.

The whole point here is to rationalize all your deployement in two quick steps:
1. Copy the template : `cp env_template .env`
2. Configure the service stack options by filling-up all the mandatory environement variables in `.env`
3. Run the `start.sh` script on a manager node of your cluster

Simple, isn't it ?