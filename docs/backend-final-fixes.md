# Correções backend — rodada B, 10/11/2026

Responsável: subagente real `/root/final_backend_fixes`, proprietário apenas de backend e deste registro. Sem edição Android nem relatórios independentes. Alterações são correções obrigatórias; zero melhorias opcionais.

- S-B1: heartbeat e telemetria (`battery`, `help`, `connectivity`, `geofence_exit`) exigem proprietário. Vínculo de leitura não pode mascarar ausência de comunicação ou produzir falsos eventos do aparelho. Teste verifica 403 e banco inalterado.
- S-B2: cron, entrega e reconciliação usam a confirmação mais recente por instante real, recebimento e desempate de inserção. Correção `taken` → `not_yet` deixa de ser ocultada por um `taken` histórico. Eventos permanecem imutáveis. Aviso cancelado ainda pendente pode voltar ao estado pending dentro da janela de cron, sem reescrever notificações já entregues. `taken` offline mais antigo não cancela aviso após correção posterior. Teste percorre todos esses estados.
- S-B3: paginação opaca `(receivedAt,id)` com `nextCursor` e `hasMore`; cursor validado, escopos mantidos, ordenação determinística inclusive timestamps iguais. Regressão insere 1003 eventos no mesmo instante e recupera todos em três páginas sem duplicatas. Contrato enviado ao coordenador para cliente Android persistir após aplicação.
- S-B4: validação de UUID/título/instante/campos textuais de consulta bloqueia payload capaz de interromper pull Android. Teste rejeita inválido e aceita consulta fictícia completa.
- Leitura direta de planos verifica consentimento de saúde também para proprietário, consistente com sync. Exportação deliberada do proprietário continua disponível.

## Evidência

`npm test --prefix backend`: **20 testes aprovados, zero falhas**, 10/11/2026. Inclui 16 anteriores + quatro regressões da rodada B. `node --check` passou para API e scheduler. SQLite local real e módulos de produção usados; nenhuma rede, conta, FCM, dispositivo Android ou implantação exercitada. Compatibilidade de cliente Android depende da integração do coordenador e compilação futura.
