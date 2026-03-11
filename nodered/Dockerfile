FROM nodered/node-red@sha256:4b51d083b403c603bea91a35a0d817c6c56e98bb93d52373b7af855ece5bf887

# Copia o arquivo de fluxos para dentro do container
COPY flows.json /data/flows.json

# Se você usa nodes extras (ex: dashboard, mysql), instale-os aqui
RUN npm install node-red-dashboard node-red-node-mysql

USER node-red