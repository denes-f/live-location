from datetime import datetime

from sqlalchemy import Column, DateTime, Float, Integer, String
from app.db.base import Base


class Location(Base):
    __tablename__ = "locations"

    id = Column(Integer, primary_key=True, index=True)
    device_id = Column(String, index=True, nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow, index=True, nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    speed = Column(Float, nullable=True)      # m/s, optional
    accuracy = Column(Float, nullable=True)   # meters, optional