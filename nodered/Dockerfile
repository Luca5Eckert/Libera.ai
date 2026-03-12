FROM nodered/node-red@sha256:4b51d083b403c603bea91a35a0d817c6c56e98bb93d52373b7af855ece5bf887

# Install extra nodes
USER root
RUN npm install node-red-dashboard node-red-node-mysql

# Copy flow and credentials files
COPY --chown=node-red:node-red flows.json /data/flows.json
COPY --chown=node-red:node-red flows_cred.json /data/flows_cred.json

USER node-red
