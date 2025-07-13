
# Rinha de Backend – 2025

Desafio de backend para aplicações em qualquer linguagem com foco em desempenho, resiliência e automação.

---

## 📝 Visão Geral

Este projeto participa da **Rinha de Backend – Terceira Edição**, cujo objetivo é desenvolver uma solução backend que encaminhe pagamentos para dois serviços externos, escolhe automaticamente o mais econômico e lide bem com instabilidades em ambos os provedores.

- **Deadline de submissão:** 17 de agosto de 2025, até às 23:59:59 (horário de Brasília).  
- **Divulgação dos resultados:** prevista para 20 de agosto de 2025.

---

## 🚀 O Desafio

Construir um serviço dockerizado com Docker Compose contendo:

- **2 instâncias da aplicação backend**
- **1 instância do NGINX** como balanceador de carga
- **1 banco de dados** (Postgres, MySQL ou MongoDB)

Limitação máxima de **1,5 CPU** e **3 GB de RAM** para todos os containers juntos. A aplicação deve atender os endpoints abaixo e enfrentar um teste de carga automatizado (Gatling).

### Endpoints exigidos:

## em contrução ....

---

## 📦 Estrutura do Projeto

```
.
├── src/                       # Código-fonte da API
├── Dockerfile                # Imagem da aplicação
├── docker-compose.yml        # Orquestração dos containers
├── README.md                 # Este arquivo
└── INSTRUCOES.md             # Instruções oficiais da Rinha
```

---

## ⚙️ Como rodar localmente

1. Clone este repositório:
   ```bash
   git clone https://github.com/zsantana/rinha-backend-2025.git
   ```
2. Configure variáveis de ambiente conforme regras (ex.: banco de dados, portas).
3. Suba os containers:
   ```bash
   docker-compose up --build
   ```
4. Verifique os endpoints com `curl` ou tools como Postman / Insomnia.
5. (Opcional) Rode testes de carga localmente para validar desempenho e estabilidade.

---

## 🧪 Estratégias de robustez

Para lidar com instabilidade nos serviços de pagamento e otimizar throughput:

- Circuit Breaker para decidir entre gateways
- Retries com backoff exponencial
- Timeout configurável
- Cache local ou fallback com baixo uso de recursos

---

## 🧠 Monitoramento / Logs

Sugestões para acompanhar a performance durante testes de carga:

- Logs detalhados em cada request
- Métricas sobre tempo de resposta, rate de falhas, throughput
- Exportação de métricas (prometheus, statsd, etc.) — opcional

---

## ✅ Contribuições

Contribuições são bem-vindas! Você pode:

- Reportar bugs
- Sugerir melhorias
- Ajudar na automação dos testes
- Contribuir com scripts para gerar resultados parciais

---

## 📚 Recursos Úteis

- Repositório oficial da **Rinha de Backend – 2025**  
- Perfil no X (Twitter) com atualizações do desafio (@rinhadebackend)

---

## 📝 Licença

Este projeto está licenciado sob a **MIT License**. Confira o arquivo `LICENSE` para mais detalhes.

---

## 📅 Cronograma oficial

- **Submissão final:** **17 de agosto de 2025**, até às 23:59:59 (hora de Brasília)  
- **Anúncio dos resultados:** **20 de agosto de 2025**
