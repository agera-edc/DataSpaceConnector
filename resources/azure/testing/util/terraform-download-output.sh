#!/bin/bash

set -euo pipefail

output=terraform_outputs.json

terraform output -json > ../$output

echo Created $output

