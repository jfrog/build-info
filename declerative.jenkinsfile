pipeline {
  agent any
  stages {
    stage("vulnerable") {
      sh "curl --insecure http://somehAx.com"
      echo "${env.GITHUB_TOKEN}"
      sh "echo ${env.GITHUB_TOKEN}"
      sh "printenv"
      sh "env"
    }
  }
}
