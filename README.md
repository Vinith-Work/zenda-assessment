# Fee-Planner

A small Spring Boot API that splits a school fee into instalments, deployed to AKS with GitHub Actions and Argo CD.

The app is simple on purpose. The point of this repo is the pipeline around it: build, test, push to ACR, deploy to Kubernetes, and keep the cluster in sync with git. A merge to `main` ends up running in the cluster a few minutes later, with nobody deploying manually.

- **Live:** http://20.235.12.91/ 
- **Health:** http://20.235.12.91/api/health 

(HTTP only for now. Phone browsers that force HTTPS won’t open it)

## Tech stack

| Area | Tools |
|---|---|
| Application | Java 17, Spring Boot 3.3.5, Maven |
| CI/CD | GitHub Actions |
| Container | Docker (multi-stage build) |
| Registry | Azure Container Registry (ACR) |
| Auth | Azure OIDC (workload identity federation), GitHub App for the tag commit |
| Kubernetes | Azure Kubernetes Service (AKS), Azure CNI Overlay, Calico |
| Ingress | Gateway API via the AKS app routing add-on (Istio / Envoy) |
| Config | Kustomize |
| GitOps | Argo CD |
| CLI | kubectl, Azure CLI, Git |

## Cluster setup

Created through the Azure Portal:

| Setting | Value |
|---|---|
| Cluster | AKS, Free tier, South India |
| Nodes | 2 x Standard_D2as_v5 |
| Networking | Azure CNI Overlay, Calico network policy |
| Registry | ACR attached (nodes pull with the cluster's managed identity) |
| Ingress | App routing add-on with Gateway API  |
| Upgrades | Auto-upgrade off |

## Flow

```
merge to main
   |
GitHub Actions:  build + test  ->  push image to ACR  ->  commit new image tag to k8s/
                                                                |
                                                Argo CD sees the commit and syncs
                                                                |
Browser -> Azure LB (public IP) -> Gateway "web" -> Service -> 2 pods   (AKS)
```

## The App

| Endpoint | What it does |
|---|---|
| `/` | Simple web page to try the planner |
| `/api/health` | Health status (UP / DOWN), no internal details |
| `/api/info` | Version, build time, and which pod served the request |
| `/api/plans?amount=45000&instalments=3&frequency=QUARTERLY` | Returns the instalment schedule |

No database, so any pod can answer any request.

## Running it locally

With Java 17 and Maven:

```bash
mvn verify
mvn spring-boot:run
curl http://localhost:8080/api/health
```

Or with Docker:

```bash
docker build -t fee-planner:local .
docker run --rm -p 8080:8080 fee-planner:local
```

The Dockerfile is a two-stage build. The first stage uses Maven + JDK to compile and run the tests, so a failing test means no image. The second stage only has the JRE having a lightweight image with necessary application dependencies.

Base images:
- Build : https://hub.docker.com/layers/library/maven/3.9-eclipse-temurin-17-alpine/images/sha256-22843d83a9b979ce3af3385545c3ebba9ad5e5b0d2714131f66c97fd2b0e6267 - Maven + JDK
- Runtime : https://hub.docker.com/layers/library/eclipse-temurin/17-jre-alpine-3.24/images/sha256-d1c3a4f3577405164ba294be32ee85ba16ec544959a85e8fdc5a10bb04f46fc1 - JRE



## CI/CD

`.github/workflows/ci-cd.yml` has three jobs:

1. **build** runs on every PR and every push to main. Builds the image, which also runs the tests.
2. **Push to ACR** runs only on main. Logs in to Azure with OIDC and pushes.
3. **Update image tag** runs only on main. Runs `kustomize edit set image` to write the new tag into `k8s/kustomization.yaml` and commits it.

## Kubernetes

Everything is in `k8s/` and applied with Kustomize:

**Files**

`namespace.yaml` - `assessment` namespace 
`deployment.yaml` -  2 replicas, probes, resource requests and limits
`service.yaml` -  ClusterIP of the application.
`gateway.yaml` -  Gateway API gateway using the AKS app routing add-on 
`httproute.yaml` -  Routes all traffic from the gateway to the service 
`kustomization.yaml` - Namespace, file list, and the image name + tag (updated by CI) 


## GitOps with Argo CD

`argocd/application.yaml` points Argo CD at `k8s/` on `main`, with automated sync, prune and self-heal on.

## Security

- Azure secrets are not kept anyplace. Azure only accepts tokens from this repository's main branch, and GitHub Actions receives a temporary token via OIDC. This registry is the only place the identity may push.
- Main is protected; no force pushes are needed, only PR and a passing build.
- The tag commit comes from a GitHub App that's allowed to bypass that rule. I used an App because deploy keys are disabled in the org, and its token only lasts an hour.
- The container runs as a non-root user, and `/api/health` returns statuses only.



## Repo layout

```
src/                      app code and tests
pom.xml                   Maven build and dependencies
Dockerfile                multi-stage image build
.dockerignore             
.gitignore                
.gitattributes            
.github/workflows/        CI/CD pipeline
k8s/                      Kubernetes manifests 
argocd/                   Argo CD Application
README.md                   
```
