#
#  Copyright (c) 2022 Microsoft Corporation
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Microsoft Corporation - initial API and implementation
#
#

FROM openjdk:11-jre-slim
ARG JAR

RUN apt-get update && apt-get install -y \
  bash \
  && rm -rf /var/lib/apt/lists/*

COPY $JAR app.jar

# Use "bash" (instead of default "sh") to support environment variables containing a dot. 
# Use "exec" for Kubernetes graceful termination (SIGINT) to reach JVM.
ENTRYPOINT [ "bash", "-c", \
    "exec java -jar $JVM_ARGS app.jar"]
