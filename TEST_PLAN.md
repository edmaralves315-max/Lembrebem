# Plano de verificação

## Executado neste ambiente
- `./scripts/check.sh`: XML, paridade cinco idiomas, referências R.string, permissões proibidas e testes backend SQLite/API/cron/JWT reais; saída em docs/evidence/local-checks.txt.
- Revisões A/B por dois agentes independentes; inspeção Kotlin/contratos adicional. Testes estáticos não comprovam execução Android.

## Workflow preparado, ainda não executado
Java17/Gradle8.11.1/SDK35: testDebugUnitTest, assembleDebugAndroidTest, lintDebug, assembleDebug. Falha em qualquer tarefa impede publicar APK. Relatórios são publicados mesmo em falha. Não há credenciais para iniciar execução GitHub neste ambiente.

## Exige aparelho/emulador
Execute `gradle :app:connectedDebugAndroidTest` em instalação de desenvolvimento com emulador/dispositivo próprio. Os testes existentes são focados, não cobertura completa. Verifique nos cinco idiomas, fonte2,320/360dp e TalkBack: ajuda visível, dose por notificação, botões clínicos, falta de voz offline e negação de permissões. Confirme alarmes às08h sem rede, reboot/fuso/DST, atraso/adiação, mudança de plano, tela bloqueada e fabricantes com economia agressiva.

Em dois Androids com Firebase/Worker configurados: convite/consentimento, revogação, dois cuidadores idiomas distintos, dose no horário +15min sem confirmação, confirmação offline/reconciliação/correção, push duplicado/atrasado, exportação/exclusão, perda de rede e estado neutro heartbeat. Validar FCM HTTPv1 e cron real. Nunca classificar ausência de confirmação como dose perdida.

Geofence: entrada/saída, demora real, permissão aproximada/background negada, posição inexistente/antiga com timestamp e precisão; mapa só cuidador. Bateria cruza30/15 uma vez e carregamento real, não botão. Voz/documentos: microfone só após consentimento/toque, background/rotação/interrupção, reprodução/Keystore/retention, OCR ilegível e importação manual, revisão nunca ativa plano automaticamente. Use exclusivamente dados fictícios.
