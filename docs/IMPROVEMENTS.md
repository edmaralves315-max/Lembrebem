# Duas rodadas de melhoria — ciclo encerrado

Revisores independentes reais: review_usability/review_security (A), usability_b/security_b (B). Arquivos originais preservados em docs/reviews. A foi revisão preparatória por código e backend executável; o marco de app Android integrado executável não foi demonstrado. B comparou correções e exercitou backend; nenhuma rodada substitui testes físicos. RF15 é parcial por essa limitação, apesar da delegação e das duas revisões efetivas.

## Rodada A
| Achado | Decisão/resultado | Evidência e limites |
|---|---|---|
| S-A1 consentimentos acoplados | Corrigido Android, regras por categoria | RulesTest adicionado, não executado; inspeção Sync/Rules |
| S-A2 acesso posterior à retirada de consentimento | Corrigido backend | testes retiram saúde/localização/escopo/preferência |
| S-A3 aviso pendente após confirmação | Corrigido backend | regressão confirmação tardia e zero entrega obsoleta |
| S-A4 evento geofence sem posição trava fila | Corrigido contrato + ambos módulos | API explícita locationUnavailable; teste evento+bateria; Android sem execução |
| S-A5 recorrência após confirmação | Corrigido Android | regra e teste JVM adicionados, não executados |
| UA1 ajuda fora da tela | Corrigido Android | ajuda fixa, teste instrumentado presente não executado |
| UA2 notificação não abre dose | Corrigido Android | PendingIntent/destino, teste instrumentado não executado |
| UA3 falta TTS sem feedback | Corrigido Android | bipe/estado/instalação por inspeção; som físico pendente |
| UA4 seletores amigáveis | Adiado | preservados formatos explícitos; futuro UX, sem mudar horários |
| UA5 retenção clínica corta opções | Aceita; aplicada na consolidação B | coluna, botões amplos,22sp; inspeção5/5, layout físico pendente |

Opcionais aceitas A: uma (UA5), abaixo do limite3. Correções obrigatórias não consomem cota. UA4 adiada por esforço/ausência de SDK, não abandona cadastro funcional existente.

## Rodada B
| Achado | Decisão/resultado | Evidência e comparação |
|---|---|---|
| UB1/2/3 verificação UA1/2/3 | Confirmadas em código, não aparelho | relatórios usability-B/android-fixes |
| UB4 UA5 | Implementada e verificada estaticamente | retenção não usa linha apertada; tipografia22sp |
| UB5 localização indisponível oculta | Corrigido UI | no_location explícito quando locationUnavailable; sem mapa inventado |
| SB1 leitor envia telemetria | Corrigido backend | teste read-only nega heartbeat/battery/help; owner envia |
| SB2 correção taken→not_yet | Corrigido backend | evento mais recente controla cron e reconciliação; histórico preservado |
| SB3 primeiros500 bloqueiam sync | Corrigido API/Android |1003 eventos mesmo timestamp recuperados sem duplicatas na API; Android cursor após aplicação, páginas limitadas/retry; sem execução Android |
| SB3 consulta inválida quebra pull | Corrigido validação API | payload malformado rejeitado; teste regressão |

Testes backend:12 antes A,16 após A,20 após B; falhas obrigatórias reproduzidas e cobertas. Opcionais novas B: zero. Mudanças de layout não alteram prescrições, escopo de vigilância ou emergência. Sem serviços pagos. Ciclo limitado a A/B; ideias novas ficam futuras. A aceitação Android permanece condicionada ao build e testes físicos em TEST_PLAN, sem alegação de validação por presença de código.
