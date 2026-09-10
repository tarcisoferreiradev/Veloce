# Veloce

Rastreador Android offline-first em Kotlin, com Compose, Room/WAL, Fused Location e MapLibre Native. Sem cadastro, backend, chave de mapa ou dependência de internet durante a atividade. Requer Android 8.0+ e Google Play Services para o provedor de localização.

## Abrir e executar

1. Extraia o ZIP e abra a pasta **Veloce** em **Android Studio → Open**.
2. Configure o **Gradle JDK como JDK 17**. Instale **Android SDK Platform 35** e **Build Tools 35.0.0** no SDK Manager. Use Android Studio compatível com AGP 8.9.2 ou superior.
3. Aguarde o Gradle Sync. A primeira compilação precisa de internet para baixar Gradle e dependências. O Gradle Wrapper oficial está incluído, com checksum da distribuição fixado.
4. Execute o módulo **app** em um celular com localização habilitada, ou emulador com Google Play Services.
5. Toque em **Iniciar nova atividade**, permita **localização precisa** e, preferencialmente, notificações. No emulador, injete coordenadas em Extended Controls → Location.
6. Caminhe em local aberto. A tela mostra distância, ritmo médio, tempo e rota. Bloquear a tela não encerra o serviço. Finalize pelo botão do app ou pela notificação.

No Windows, na raiz do projeto:

```powershell
.\gradlew.bat :core:test :domain:test :app:assembleDebug
```

No Linux/macOS:

```bash
chmod +x gradlew
./gradlew :core:test :domain:test :app:assembleDebug
```

APK gerado após uma compilação bem-sucedida: `app/build/outputs/apk/debug/app-debug.apk`.

Para validar o banco em um dispositivo/emulador conectado:

```bash
./gradlew :data:connectedDebugAndroidTest
```

O Android Studio cria `local.properties` com o caminho do seu SDK. Esse arquivo é específico da sua máquina e não está incluído.

## Organização

| Módulo/pacote | Responsabilidade | Dependências internas |
| --- | --- | --- |
| `core` / `com.veloce.core` | Haversine, pace e formatação; Kotlin/JVM puro | Nenhuma |
| `domain` / `com.veloce.domain` | Entidades, contratos, erros, motor de captura e caso de uso da rota | `core` |
| `data` / `com.veloce.data` | Room, DAO, mapeadores, repositório e adaptador Fused Location | `domain` |
| `app` / `com.veloce.presentation` | Compose, ViewModel, MapLibre, permissões e Foreground Service | `domain`, `core` |
| `app` / `com.veloce.AppGraph` | Composition root com injeção por construtor | Conecta `data` e `presentation` |

O módulo Android `app` depende de `data` apenas para montar o grafo de objetos. Os arquivos de `presentation` não importam classes de `data`. O domínio não conhece Room, Context, Compose, Google ou MapLibre. Implementações Android de portas ficam nas camadas externas; `core` permanece independente de plataforma.

Kotlin utiliza `val` e coleções de leitura, em vez de uma palavra-chave `readonly`. Estado mutável necessário é privado e encapsulado. Não há GlobalScope, banco na thread principal ou singleton global de Activity.

## Fluxo de execução

`Activity → TrackingController → Foreground Service → TrackingEngine → LocationSource → ITrackingRepository → Room`

O DAO publica Flow; o caso de uso acumula a distância dos segmentos novos; o ViewModel combina rota, sessão e StateFlow do motor; Compose observa o estado com respeito ao lifecycle.

Apenas a Activity visível inicia o serviço, depois da concessão da permissão. O manifesto declara `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` e `foregroundServiceType="location"`. Não é solicitada localização permanente em segundo plano. A recusa de notificações não impede a captura quando a localização precisa foi concedida.

## Banco local

- Arquivo privado `veloce.db`, versão 1, journal **WRITE_AHEAD_LOGGING**.
- `location_points`: `id`, `session_id`, `latitude`, `longitude`, `altitude`, `speed`, `timestamp`.
- `sessions`: identificador, início, término e duração monotônica consolidada.
- Índice único `(session_id, timestamp)` torna a repetição de uma amostra idempotente. As consultas retornam ordem `timestamp, id`.
- FK para `sessions`; gravação de ponto e checkpoint de duração na mesma transação.
- Finalizar uma sessão impede novas inserções nela pelo método transacional.
- Room exporta o schema para `data/schemas` na primeira compilação. Preserve os JSONs gerados no controle de versão e implemente migrações ao mudar a versão. Não há fallback de migração destrutiva.
- Backup automático desabilitado. A telemetria fica no armazenamento privado do app; desinstalar o aplicativo remove esses dados. Este projeto não implementa exportação ou sincronização remota.

`Outcome` e `TrackingFailure` tipam disco cheio, corrupção, contenção, indisponibilidade de I/O, dados inválidos, permissão e falhas de localização. Cancelamentos de Coroutines são propagados. Apenas contenção de banco recebe duas novas tentativas curtas; outros erros interrompem a captura e chegam à UI. Não há descarte silencioso quando a fila GPS de 256 amostras transborda.

## Métricas e GPS

- Latitude/longitude em graus WGS84; altitude em metros; velocidade em m/s; timestamp UTC em milissegundos.
- Altitude e velocidade são `null` quando o provedor não as oferece.
- Solicitação de alta precisão, intervalo desejado de 2 s, mínimo de 1 s e deslocamento mínimo de 3 m. O sistema pode ajustar a frequência efetiva.
- Ignora amostras com precisão pior que 30 m, antigas além de 15 s ou repetidas segundo o relógio monotônico do GPS.
- Haversine usa raio médio de 6.371.008,8 m, limita erros numéricos do arco e soma os novos segmentos incrementalmente. Não usa altitude para a distância horizontal.
- O HUD mostra **ritmo médio**, calculado pelo tempo total decorrido/distância. Paradas entram no tempo; não há pausa automática. Zero, valores não finitos e ritmos acima de 40 min/km aparecem como `—`; não são artificialmente limitados a 40.
- Duração usa `SystemClock.elapsedRealtime()`, continua com tela bloqueada e não depende de ajustes no relógio civil. `HH:MM:SS` permite horas superiores a 24.
- Fontes monoespaçadas com recurso tabular `tnum` evitam oscilação visual dos números.

## Mapa offline

`assets/offline-style.json` é um estilo local sem tiles remotos, fontes ou sprites. A rota vem dos pontos persistidos, em `GeoJsonSource` + `LineLayer`; o último ponto aparece em `CircleLayer`. O lifecycle de MapView e callbacks de baixa memória são encaminhados e liberados.

**O mapa incluído desenha a rota sobre fundo simples e não contém ruas, nomes ou cartografia de cidades.** Não há gerenciador de downloads de regiões nesta versão. Para acrescentar ruas offline, configure uma fonte/estilo de cartografia, direitos de uso e download prévio de regiões pelo mecanismo offline do MapLibre. A persistência de telemetria é independente dessa extensão.

A câmera acompanha o último ponto a cada atualização. A tela principal apresenta a sessão mais recente; sessões anteriores permanecem no banco, mas uma tela de histórico não faz parte desta entrega.

## Encerramento e recuperação

- Finalizar cancela a assinatura GPS, remove callbacks, grava o término e retira a notificação.
- Girar a tela, abrir outra tela ou remover a tarefa dos recentes não solicita a parada do serviço.
- Nenhum Foreground Service pode garantir sobrevivência a force-stop, reinicialização, revogação de permissão ou políticas agressivas do fabricante. O projeto usa `START_NOT_STICKY`: não inicia uma atividade sem ação explícita do usuário após o processo morrer.
- Ao reabrir, uma sessão sem término aparece como interrompida com os dados já gravados. Ao iniciar outra, o motor fecha a anterior no último checkpoint conhecido e cria um novo identificador. Não liga os trajetos separados por um intervalo sem GPS.
- Se o processo morrer antes do primeiro ponto, não há duração intermediária persistida. Se morrer depois, o tempo recuperado é o checkpoint do último ponto. Tempo e trecho não observados não são reconstruídos.
- Pontos ainda em trânsito no momento de uma interrupção podem não ter sido consolidados. Uma falha de armazenamento é visível; dados já confirmados não são apagados.

## Validação desta entrega

**Executado:** compilação dos fontes e testes de `core` e `domain` com Kotlin 2.1.20/JVM 17 e execução por JUnit 4.13.2: **9 testes aprovados**. Incluem Haversine/dateline/antípodas, pace, duração, cancelamento com salto do relógio civil, disco cheio e distância incremental. O checksum do JAR oficial do Gradle Wrapper foi conferido.

**Não executado:** build Android completo, lint Android, teste instrumentado de Room e execução em celular. O ambiente de criação não possuía Android SDK/JDK completo e o Gradle Wrapper não conseguiu baixar a distribuição pela rede do processo Java. A tentativa de `:app:assembleDebug` parou antes de compilar código Android. Portanto, não há APK incluído nem afirmação de compilação Android validada.

O teste instrumentado fornecido verifica WAL em banco de arquivo, persistência após reabertura, ordenação, duplicação e bloqueio de inserções em sessão finalizada. Execute-o em dispositivo de teste: ele limpa o banco `veloce.db` do alvo de testes.

## Validação no celular

1. Grave um trajeto com rede desligada e localização habilitada; confira os pontos após reabrir.
2. Bloqueie a tela durante alguns minutos, caminhe e confirme a continuidade.
3. Gire a tela e volte pelo aviso da notificação; não deve surgir outra sessão.
4. Finalize pela notificação; a contagem deve parar e os dados continuar disponíveis.
5. Negue localização precisa ou desligue o GPS; confira a mensagem de erro e ausência de captura falsa.
6. Interrompa o processo durante uma sessão e reabra; confira a indicação de interrupção e a criação de uma sessão separada ao reiniciar.

## Referências das APIs e dependências

- [Tipos e requisitos de Foreground Service](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [Room](https://developer.android.com/jetpack/androidx/releases/room)
- [AGP 8.9](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [MapLibre Android](https://maplibre.org/maplibre-native/android/api/)
- [Artefato fixado MapLibre 11.8.2](https://repo1.maven.org/maven2/org/maplibre/gl/android-sdk/11.8.2/)
- [Checksums oficiais do Gradle](https://gradle.org/release-checksums/)

Versões estão fixadas nos arquivos Gradle; não usam `+` ou versões dinâmicas. O MapLibre foi fixado em 11.8.2 (OpenGL), não na versão mais recente. O wrapper oficial é distribuído sob sua licença em `gradle/LICENSE.txt`; as bibliotecas mantêm suas respectivas licenças.
