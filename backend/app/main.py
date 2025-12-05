from fastapi import FastAPI

from app.api.v1.routes_locations import router as locations_router
from app.core.config import settings
from app.db.base import Base
from app.db.session import engine

def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.PROJECT_NAME,
    )

    # Include API routers
    app.include_router(
        locations_router,
        prefix=settings.API_V1_PREFIX,
    )

    return app


app = create_app()


# Create tables on startup (for dev only; later use Alembic)
@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)