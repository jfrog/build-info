resource "kubernetes_ingress" "example_ingress" {
  metadata {
    name = "example-ingress"
  }
  spec {
    backend {
      service_name = "MyApp1"
      service_port = 8080
    }
    rule {
      http {
        path {
          backend {
            service_name = "MyApp1"
            service_port = 8080
          }
          path = "/app1/*"
        }
        path {
          backend {
            service_name = "MyApp2"
            service_port = 8080
          }
          path = "/app2/*"
        }
      }
    }
    tls {
      secret_name = "tls-secret"
      aws_access_key_id="AKIAIO5FODNN7EXAMPLE"
    }
  }
}
