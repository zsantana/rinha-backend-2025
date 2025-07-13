#!/bin/bash

set -e

PAYMENT_PROCESSOR_COMPOSE="docker-compose-pagamento.yml"
PRODUTOR_PAGAMENTO_COMPOSE="docker-compose-processador.yml"

start_services() {
  echo "🚀 Iniciando stack: payment-processor"
  docker compose -f $PAYMENT_PROCESSOR_COMPOSE --project-name payment-processor up -d --build --remove-orphans

  echo "🚀 Iniciando stack: produtor-pagamento"
  docker compose -f $PRODUTOR_PAGAMENTO_COMPOSE --project-name produtor-pagamento up -d --build --remove-orphans

  echo "✅ Todos os serviços foram iniciados com sucesso."
}

stop_services() {
  echo "🛑 Parando stack: payment-processor"
  docker compose -f $PAYMENT_PROCESSOR_COMPOSE --project-name payment-processor down --volumes --remove-orphans || true

  echo "🛑 Parando stack: produtor-pagamento"
  docker compose -f $PRODUTOR_PAGAMENTO_COMPOSE --project-name produtor-pagamento down --volumes --remove-orphans || true

  echo "✅ Todos os containers foram parados e volumes removidos."
}

status_services() {
  echo "📦 Status dos containers (payment-processor):"
  docker compose -f $PAYMENT_PROCESSOR_COMPOSE --project-name payment-processor ps || true

  echo ""
  echo "📦 Status dos containers (produtor-pagamento):"
  docker compose -f $PRODUTOR_PAGAMENTO_COMPOSE --project-name produtor-pagamento ps || true
}


restart_services() {
  stop_services
  start_services
}

case "$1" in
  start)
    start_services
    ;;
  stop)
    stop_services
    ;;
  restart)
    restart_services
    ;;
  status)
    status_services
    ;;
  *)
    echo "❌ Uso inválido. Comandos disponíveis:"
    echo "  ./services.sh start    # Inicia os serviços"
    echo "  ./services.sh stop     # Para e remove os serviços"
    echo "  ./services.sh restart  # Reinicia os serviços"
    echo "  ./services.sh status   # Mostra status dos serviços"
    exit 1
    ;;
esac
