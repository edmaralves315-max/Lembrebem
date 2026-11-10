# Segurança e confiabilidade — Rodada B

Revisor independente: `/root/security_b`, 10/11/2026. Produção somente leitura. Escopo limitado à comparação da Rodada A e três defeitos do escopo existente; não se propõem funções novas.

## Método e resultados executados

- `cd backend && npm test`: **16 testes aprovados, zero falhas**, incluindo as quatro regressões da Rodada A. Node/SQLite local; nenhum serviço externo.
- Reproduções adicionais em processo Node com banco SQLite em memória, usando `handle`, `Store` e `tick` reais. Dados fictícios; nenhum arquivo de produção alterado. Resultados abaixo.
- Inspeção de Sync.kt, Rules.kt, Alarms.kt, Monitoring.kt, ClinicalDomain/ClinicalStorage, API/scheduler e instruções persistentes.
- Não executados: compilação Android, testes JVM/instrumentados, dois aparelhos, TTS/som, boot/tela bloqueada, OCR/microfone/Keystore, geofence e FCM real. Documentação externa não consultada: o acesso de rede do ambiente permanece bloqueado por falha do proxy; conclusões se apoiam em código e testes locais.

## Comparação com a Rodada A

| Achado A | Evidência B | Situação |
|---|---|---|
| S-A1 consentimentos independentes | Rules.maySync separa saúde/localização e protege telemetria do cuidador; SyncWorker não faz mais retorno global por ausência de consentimento de saúde | Corrigido por inspeção e teste JVM escrito, ainda sem execução Android |
| S-A2 revalidação de consentimento/escopo | alertAllowed aplicado à criação/leitura/entrega; regressões de retirada de consentimento, escopo e preferência passam | Corrigido e testado no backend local |
| S-A3 aviso pendente após confirmação | API cancela pending/retry; deliver reconsulta taken; regressão de confirmação tardia passa | Corrigido e testado no backend local; correção posterior de confirmação ainda falha (SB2) |
| S-A4 ausência de localização bloqueava fila | Contrato locationUnavailable combinado, teste aceita saída sem coordenadas e bateria seguinte; Android isola HTTP permanente | Backend testado; integração Android inspecionada, não executada |
| S-A5 recorrência parava em dose tomada | AlarmReceiver agenda próxima ocorrência antes de shouldAnnounceDose | Corrigido por inspeção; teste JVM escrito, execução Android pendente |

## Achados B (três)

| ID / prioridade | Evidência observada e impacto | Correção proposta | Esforço / dependência / risco | Teste objetivo |
|---|---|---|---|---|
| SB1 Alta, autorização/telemetria | `api.mjs`: heartbeat e eventos battery/connectivity/help exigem somente `read`. Reproduzido: conta vinculada **apenas read** recebe HTTP200 ao atualizar heartbeat e postar bateria15%. Assim pode mascarar ausência de comunicação do aparelho ou produzir alertas falsos, apesar da proteção apenas no cliente Android. | Servidor deve restringir telemetria do aparelho ao proprietário/dispositivo autorizado; concessão read não pode escrever estados. Definir separadamente permissão de solicitar ajuda, se necessária. | Baixo/médio; backend; baixo com rejeição explícita e testes de papéis. | Grant somente read recebe403 para heartbeat/battery/connectivity; owner continua200; timeout não é renovado por leitor. |
| SB2 Alta, reconciliação de dose | `scheduler.mjs` usa `some(status==='taken')` sobre todo histórico e `takenAcknowledgement` retorna qualquer taken. Reproduzido: taken08:01 seguido de correção auditada not_yet08:02, cron08:15 produz **zero avisos sem confirmação**. Android mostra último estado por occurredAt, mas backend conserva tomada para sempre. | Calcular estado efetivo por dose a partir do último evento, com desempate estável; manter histórico e correção auditável. Não inventar dose perdida. | Médio; backend e contrato de ordenação Android; risco moderado por semântica clínica, restringir à correção já prevista e testar. | taken→not_yet mais recente resulta estado efetivo not_yet e aviso sem confirmação; inverso cancela aviso; eventos offline fora de ordem usam occurredAt real e desempate documentado. |
| SB3 Alta, sincronização longa | `/sync` retorna primeiros500 eventos desde1970 por padrão; SyncWorker sempre chama sem cursor. Reproduzido banco504 eventos retorna500. Após limite, novos eventos de confirmação/consulta/localização nunca chegam ao Android. Além disso, appointment aceita payload sem validação de campos e o pull faz getString obrigatório; evento malformado pode provocar retry permanente. | Cursor/paginação estável por receivedAt+id, persistido apenas após aplicação; validar payload appointment e tratar rejeição individual sem paralisar itens válidos. | Médio; backend+Android; baixo/médio com teste de mais de500 e timestamps iguais. | Sincronizar501+ eventos em páginas sem perder/duplicar; último evento aparece. Appointment sem id/title/at é400; evento válido seguinte continua aplicável. |

Todos são defeitos dos requisitos existentes e não consomem cota de melhorias opcionais. São achados sobre o estado inspecionado, não alegação de correção: o coordenador deve registrar decisões e testes após eventual alteração. A rodada B encerra a descoberta; não iniciar rodada ilimitada.

## Controles preservados e limites

Push genérico privado; autorização e convites revogáveis; RSA Firebase; idempotência e rate limit; localização indisponível explícita; nenhum caminho clínico de IA promove medicação automaticamente; voz/documentos locais protegidos por Keystore. Esses pontos foram inspecionados e parte do backend testada; não equivalem a certificação clínica, auditoria completa nem prova de operação física/offline em Android.

## Reinspeção das correções B — mesma rodada, estado final

Correções feitas pelo implementador `/root/final_backend_fixes` e coordenador; este revisor não alterou produção. Nova execução independente `npm --prefix backend test`: **20/20 aprovados, zero falhas**, incluindo quatro regressões novas. Reinspeção de API, scheduler e Sync.kt:

| Achado | Verificação após correção | Resultado e limite |
|---|---|---|
| SB1 | `owner()` aplicado a heartbeat e battery/help/connectivity/geofence_exit; regressão cria leitor e verifica403 e banco inalterado | Corrigido e testado backend local |
| SB2 | latestAcknowledgement ordena occurredAt DESC, receivedAt DESC e rowid DESC; cron/entrega/reconciliação usam estado efetivo; teste taken→not_yet e taken offline anterior passa | Corrigido e testado backend local. Android compara occurredAt estritamente maior; empate exato de carimbo entre duas correções permanece caso de teste físico/integração não coberto |
| SB3 paginação | API expõe cursor validado `(receivedAt,id)` e hasMore; teste recupera1003 eventos com timestamp igual sem duplicatas. SyncWorker percorre páginas, persiste cursor depois de aplicar e solicita retry se exceder10 páginas | Backend testado; integração de paginação Android inspecionada, sem compilação/execução. Concorrência de inserção e mudança de scopes durante paginação exigem ensaio adicional antes de produção |
| SB3 consulta inválida (regressão S-B4) | API valida UUID/título/instante/campos textuais antes de gravar; teste inválido400 e válido200 | Corrigido e testado backend local |

Resultado: as reproduções críticas da rodada B receberam correção e regressão executável de backend. Não há alegação de APK compilado nem de teste Android. Limitações remanescentes devem constar do relatório final; encerrada a rodada B, sem novas melhorias opcionais.
