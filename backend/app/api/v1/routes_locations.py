from typing import List

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.location import Location as LocationModel
from app.schemas.location import Location, LocationBatchCreate


router = APIRouter(tags=["locations"])


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@router.post("/locations", response_model=List[Location])
def create_locations(
    payload: LocationBatchCreate,
    db: Session = Depends(get_db),
):
    created = []
    for loc in payload.locations:
        obj = LocationModel(
            device_id=loc.device_id,
            latitude=loc.latitude,
            longitude=loc.longitude,
            speed=loc.speed,
            accuracy=loc.accuracy,
            timestamp=loc.timestamp,
        )
        db.add(obj)
        created.append(obj)
    db.commit()
    for obj in created:
        db.refresh(obj)
    return created