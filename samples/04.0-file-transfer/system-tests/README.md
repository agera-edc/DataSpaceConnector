# File transfer system tests

This system test serves to validate both the connector runtime and the EDC Helm chart with an end-to-end flow.
It runs within GitHub Actions.

To run it locally, install:
- [Minikube and Kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/get-docker/)

## Install helm release with minikube

Start minikube:

```bash
minikube start
```

[Set minikube docker environment](https://minikube.sigs.k8s.io/docs/handbook/pushing/#1-pushing-directly-to-the-in-cluster-docker-daemon-docker-env).

```bash
eval $(minikube docker-env)
```

Run tests:

```bash
cd DataspaceConnector
samples/04.0-file-transfer/system-tests/kubernetes-test.sh
```
