#/bin/sh

set -euxo pipefail

dir=$(dirname $0)

# Build and install Consumer and Provider connectors

for target in consumer provider; do
  docker build -t $target --build-arg JAR=samples/04.0-file-transfer/$target/build/libs/$target.jar -f launchers/generic/Dockerfile .
  helm upgrade --install -f $dir/values-$target.yaml $target charts/dataspace-connector
done

# Wait for pods to be live

for target in consumer provider; do
  kubectl wait --for=condition=available deployment $target-dataspace-connector --timeout=120s
done

# Resolve NodePort address for Consumer

nodeIP=$(kubectl get nodes --namespace default -o jsonpath="{.items[0].status.addresses[0].address}")
consumerPort=$(kubectl get --namespace default -o jsonpath="{.spec.ports[0].nodePort}" services consumer-dataspace-connector)

# Perform negotiation and file transfer. See sample root directoy README.md file for more details.

consumerUrl="http://$nodeIP:$consumerPort"
providerUrl="http://provider-dataspace-connector"
destinationPath="/tmp/destination-file-$RANDOM"
apiKey="password"

./gradlew :samples:04.0-file-transfer:client:run --args "$consumerUrl $providerUrl $destinationPath $apiKey"

kubectl exec deployment/provider-dataspace-connector -- wc -l $destinationPath
echo "Test succeeded."
