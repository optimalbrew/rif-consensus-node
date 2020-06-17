FROM openjdk:11.0.2-jre-slim-stretch
# FROM amazoncorretto:11.0.7

COPY rif-consensus-node /opt/rif-consensus-node/
RUN chmod 700 /opt/rif-consensus-node/bin/rif-consensus-node
WORKDIR /opt/rif-enterprise

# Expose services ports
# 8545 HTTP JSON-RPC
# 8546 WS JSON-RPC
# 8547 HTTP GraphQL
# 30303 P2P
EXPOSE 8545 8546 8547 30303
# EXPOSE 5005   Debug Port

# defaults for host interfaces
ENV BESU_RPC_HTTP_HOST 0.0.0.0
ENV BESU_RPC_WS_HOST 0.0.0.0
ENV BESU_GRAPHQL_HTTP_HOST 0.0.0.0

ENV PATH="/opt/rif-consensus-node/bin:${PATH}"
ENTRYPOINT ["rif-consensus-node"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="RIF Consensus Node" \
      org.label-schema.description="Enterprise Ethereum client" \
      org.label-schema.url="https://besu.hyperledger.org/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/rsksmart/rif-consensus-node.git" \
      org.label-schema.vendor="RIF" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"