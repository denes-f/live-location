from datetime import datetime
from typing import Optional, List

from pydantic import BaseModel, Field


class LocationBase(BaseModel):
    device_id: str = Field(..., example="phone-01")
    latitude: float = Field(..., ge=-90, le=90, example=47.4979)
    longitude: float = Field(..., ge=-180, le=180, example=19.0402)
    speed: Optional[float] = Field(None, example=1.2)
    accuracy: Optional[float] = Field(None, example=5.0)
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class LocationCreate(LocationBase):
    pass


class Location(LocationBase):
    id: int

    class Config:
        from_attributes = True


class LocationBatchCreate(BaseModel):
    locations: List[LocationCreate]