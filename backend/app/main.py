from fastapi import FastAPI

from app.api.v1.routes_locations import router as locations_router
from app.core.config import settings
from app.db.base import Base
from app.db.session import engine
from fastapi import FastAPI, Depends, Request
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.location import Location

def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.PROJECT_NAME,
    )

    # Include API routers
    app.include_router(
        locations_router,
        prefix=settings.API_V1_PREFIX,
    )

    def get_db():
        db = SessionLocal()
        try:
            yield db
        finally:
            db.close()
    templates = Jinja2Templates(directory="app/templates")

    @app.get("/locations-view")
    def show_locations(request: Request, db: Session = Depends(get_db)):
        items = db.query(Location).order_by(Location.timestamp.desc()).all()
        return templates.TemplateResponse(
            "locations.html",
            {"request": request, "locations": items}
        )

    return app


app = create_app()


# Create tables on startup (for dev only; later use Alembic)
@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)