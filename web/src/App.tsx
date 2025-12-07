import { useEffect, useMemo, useState } from 'react'
import { MapContainer, TileLayer, Marker, Polyline, Popup } from 'react-leaflet'
import type { LatLngExpression } from 'leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'

type LocationPoint = {
  id: number
  device_id: string
  latitude: number
  longitude: number
  timestamp: string
  speed?: number | null
  accuracy?: number | null
}

// Fix default marker icons for Leaflet when bundled (Vite / Webpack)
L.Marker.prototype.options.icon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
})

const DEFAULT_CENTER: LatLngExpression = [47.4979, 19.0402] // Budapest
const DEFAULT_ZOOM = 13

const COLOR_PALETTE = [
  '#e53935',
  '#8e24aa',
  '#3949ab',
  '#1e88e5',
  '#00897b',
  '#7cb342',
  '#fdd835',
  '#fb8c00',
  '#6d4c41',
]

function App() {
  const [locations, setLocations] = useState<LocationPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchLocations = async () => {
      try {
        setLoading(true)
        setError(null)

        const res = await fetch('/api/v1/locations')
        if (!res.ok) {
          throw new Error(`Failed to load locations: ${res.status}`)
        }

        const data: LocationPoint[] = await res.json()
        setLocations(data)
      } catch (err: any) {
        console.error(err)
        setError(err.message ?? 'Unknown error while loading locations')
      } finally {
        setLoading(false)
      }
    }

    fetchLocations()
  }, [])

  const deviceTracks = useMemo(() => {
    const grouped: Record<string, LocationPoint[]> = {}

    for (const loc of locations) {
      if (!grouped[loc.device_id]) {
        grouped[loc.device_id] = []
      }
      grouped[loc.device_id].push(loc)
    }

    // sort each device's points by timestamp
    Object.keys(grouped).forEach((deviceId) => {
      grouped[deviceId].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
      )
    })

    return grouped
  }, [locations])

  const deviceIds = useMemo(() => Object.keys(deviceTracks).sort(), [deviceTracks])

  const center: LatLngExpression =
    locations.length > 0
      ? [locations[0].latitude, locations[0].longitude]
      : DEFAULT_CENTER

  const getColorForDevice = (deviceId: string) => {
    const index = deviceIds.indexOf(deviceId)
    if (index === -1) return '#000000'
    return COLOR_PALETTE[index % COLOR_PALETTE.length]
  }

  return (
    <div className="app-layout">
      <div className="sidebar">
        <h2 style={{ marginTop: 0 }}>Live Location Dashboard</h2>
        <p style={{ fontSize: '0.9rem', color: '#555' }}>
          Showing locations from the backend API
          (<code>/api/v1/locations</code>).
        </p>

        {loading && <p>Loading locations…</p>}
        {error && <p style={{ color: 'red' }}>{error}</p>}

        {deviceIds.length === 0 && !loading && !error && (
          <p>No location data yet. Try sending some test points.</p>
        )}

        {deviceIds.length > 0 && (
          <div>
            <h3 style={{ fontSize: '1rem', marginTop: '1.5rem' }}>Devices</h3>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              {deviceIds.map((deviceId) => (
                <li
                  key={deviceId}
                  style={{ display: 'flex', alignItems: 'center', marginBottom: '0.5rem' }}
                >
                  <span
                    style={{
                      width: '12px',
                      height: '12px',
                      borderRadius: '50%',
                      backgroundColor: getColorForDevice(deviceId),
                      display: 'inline-block',
                      marginRight: '0.5rem',
                    }}
                  />
                  <span>{deviceId}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="map-wrapper">
        <MapContainer
          center={center}
          zoom={DEFAULT_ZOOM}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {deviceIds.map((deviceId) => {
            const points = deviceTracks[deviceId]
            if (!points || points.length === 0) return null

            const positions: LatLngExpression[] = points.map((p) => [
              p.latitude,
              p.longitude,
            ])

            const last = points[points.length - 1]
            const color = getColorForDevice(deviceId)

            return (
              <div key={deviceId}>
                <Polyline positions={positions} pathOptions={{ color, weight: 4 }} />
                <Marker position={[last.latitude, last.longitude]}>
                  <Popup>
                    <div>
                      <strong>Device:</strong> {deviceId}
                      <br />
                      <strong>Last update:</strong>{' '}
                      {new Date(last.timestamp).toLocaleString()}
                    </div>
                  </Popup>
                </Marker>
              </div>
            )
          })}
        </MapContainer>
      </div>
    </div>
  )
}

export default App
