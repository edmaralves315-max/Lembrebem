# Inspeção de integração Android — 10/11/2026

Responsável: subagente real `integration_check`. Produção examinada somente para leitura; nenhum SDK, compilador Kotlin ou dependência foi baixado. Esta inspeção não demonstra compilação e não substitui o resultado do workflow.

## Conferências

- Gradle Android referencia AGP 8.9.2, Kotlin/Compose 2.1.20 e KSP 2.1.20-1.0.32; o runner deve usar Gradle 8.11.1, JDK 17 e plataforma/build-tools 35. `compileSdk` e `targetSdk` atuais são 35; a compatibilidade de distribuição futura e Android 16 continua pendente de revisão/teste.
- O módulo clínico está chamado por `ClinicalScreen(onBack, canReview)` no fluxo principal e as dependências ML Kit Latin/Japanese e coroutines Play Services estão declaradas.
- As chamadas `Field` agora correspondem à assinatura com lambda final; o erro anteriormente relatado foi corrigido.
- Namespace `org.lembrebem`, imports de `R`, classes manifest e diretórios de testes são consistentes na inspeção. Compose instrumentado e AndroidJUnitRunner têm dependências declaradas.
- Entidades Room usam tipos primitivos persistíveis, índices de doses e banco versão 1. A exportação do schema depende da execução KSP; nenhum schema gerado está comprovado antes da compilação. Evoluções de esquema futuras exigem migração explícita.
- O contrato de sincronização usa `/v1`, bearer Firebase, `sourceReviewed`, versões positivas, UTC e IDs de ocorrência com milissegundos. A saída sem posição usa `locationUnavailable:true` tanto em Android quanto no backend. O pull evita construir localização sem coordenadas.
- Testes JVM de Rules e Clinical estão nos sourceSets corretos. Os testes instrumentados verificam persistência offline e destinos de ajuda/dose, mas ainda não foram executados.

## Ações para a primeira compilação

`requireNotNull(...)` foi aplicado aos serviços essenciais em AlarmScheduler, Alerts, ConnectivityStatus e PermissionCenter. A alteração explicita o contrato de nulabilidade dos stubs; não é alegada como correção de erro reproduzido por compilador local.

Não foi encontrado outro bloqueio de sintaxe Kotlin que possa ser afirmado com evidência do compilador neste ambiente. O workflow deverá executar testes JVM, `assembleDebug` e compilação do APK instrumentado; o primeiro resultado pode revelar problemas que análise textual não detecta. A ausência de SDK impede afirmar que o projeto compila.

## Limites funcionais mantidos explícitos

Transcrição automática não instalada: fallback manual real, sem transcrições inventadas. OCR/resumo/revisão nunca publicam prescrições automaticamente. A revisão de rascunho é local; o familiar confere o original e cadastra itens nas telas específicas. FCM, geofence, áudio, TTS e armazenamento Keystore continuam sujeitos a teste Android real. Os textos de saúde/segurança nos cinco idiomas exigem revisão humana.

## Conferência final do cursor e CI

O novo loop de `SyncWorker` foi reinspecionado: parâmetro `cursor` usa URL encoding UTF-8, `nextCursor` é tratado como token opaco, `hasMore` controla paginação e há limite de dez páginas por trabalho seguido de retry. O cursor é persistido somente depois da aplicação da página; sua chave separa endpoint, pessoa, usuário, consentimentos e papel. Reprocessar uma página após interrupção usa IDs e versões existentes. O formato coincide com a resposta API `nextCursor`/`hasMore`. Não foi observado novo erro sintático Kotlin na leitura; isso permanece análise estática, sem execução Android.

O workflow `.github/workflows/android.yml` tem dois jobs Linux, Node 24.22.1, JDK17, SDK35/build-tools35.0.0 e Gradle8.11.1 explícitos. A sequência Android inclui testes JVM, compilação instrumentada, lint e APK debug; relatórios são enviados mesmo em falha e APK exige arquivo existente. Os testes instrumentados são apenas compilados, sem emulador configurado. O workflow usa `gradle` instalado por setup-gradle e não depende de wrapper inexistente. Nada foi disparado no GitHub e nenhuma conexão externa foi feita nesta inspeção.
