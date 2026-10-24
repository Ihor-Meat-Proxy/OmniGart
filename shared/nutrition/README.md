# shared/nutrition

Adaptive calorie and macro target model (pure Kotlin Multiplatform / JVM).

See [docs/NUTRITION_MODEL.md](../../docs/NUTRITION_MODEL.md).

```bash
./gradlew :shared:nutrition:jvmTest
```

Diary storage is out of scope; hosts implement `DailyIntakeRead` / `DailyTargetRead`.
