from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    # General
    PROJECT_NAME: str = "Live Location Backend"
    API_V1_PREFIX: str = "/api/v1"

    # Database
    POSTGRES_HOST: str = "db"
    POSTGRES_PORT: int = 5432
    POSTGRES_USER: str = "live_location"
    POSTGRES_PASSWORD: str = "live_location"
    POSTGRES_DB: str = "live_location"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.POSTGRES_USER}:"
            f"{self.POSTGRES_PASSWORD}@{self.POSTGRES_HOST}:"
            f"{self.POSTGRES_PORT}/{self.POSTGRES_DB}"
        )

    # Telegram (will use later)
    TELEGRAM_BOT_TOKEN: str | None = None
    TELEGRAM_CHAT_ID: str | None = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()