import { useState, useEffect, useRef, useCallback } from 'react'
import './App.css'

const host = typeof window !== 'undefined' && window.location.hostname ? window.location.hostname : '127.0.0.1'
const WS_URL = `ws://${host}:8080`
const HTTP_URL = `http://${host}:9999/status`

// Màu xe 3D tương ứng cho từng ô đỗ
const SLOT_CAR_COLORS = {
  1: '#38bdf8', // Xanh Cyan
  2: '#f43f5e', // Đỏ Ruby
  3: '#f59e0b', // Vàng Amber
}

// Component Xe 3D Isometric SVG
function IsometricCar({ color = '#38bdf8' }) {
  return (
    <div className="car-3d-wrapper">
      {/* Vệt đèn pha chiếu sáng mặt đường */}
      <div className="headlight-beam beam-left"></div>
      <div className="headlight-beam beam-right"></div>
      
      {/* Bóng đổ gầm xe 3D */}
      <div className="car-shadow-3d"></div>

      {/* Thân xe 3D Isometric */}
      <svg className="car-svg-3d" viewBox="0 0 200 110" fill="none" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id={`carBody-${color}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#1e293b" />
            <stop offset="40%" stopColor={color} />
            <stop offset="100%" stopColor="#090d16" />
          </linearGradient>
          <linearGradient id="glassGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#0f172a" stopOpacity="0.9" />
          </linearGradient>
          <linearGradient id="glowHeadlight" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#fef08a" />
            <stop offset="100%" stopColor="#38bdf8" />
          </linearGradient>
        </defs>

        {/* Bánh xe sau & trước */}
        <ellipse cx="46" cy="74" rx="14" ry="18" fill="#020617" stroke="#334155" strokeWidth="3" />
        <ellipse cx="154" cy="74" rx="14" ry="18" fill="#020617" stroke="#334155" strokeWidth="3" />
        <ellipse cx="46" cy="74" rx="6" ry="8" fill="#64748b" />
        <ellipse cx="154" cy="74" rx="6" ry="8" fill="#64748b" />

        {/* Thân xe dưới */}
        <path d="M 24 68 Q 30 52, 60 52 L 140 52 Q 170 52, 176 68 Q 180 78, 168 80 L 32 80 Q 20 78, 24 68 Z" 
              fill={`url(#carBody-${color})`} stroke="rgba(255,255,255,0.2)" strokeWidth="1.5" />

        {/* Nóc xe & Kính chắn gió */}
        <path d="M 54 52 L 72 26 Q 78 22, 100 22 L 122 22 Q 134 22, 142 32 L 152 52 Z" 
              fill="#090d16" stroke="rgba(255,255,255,0.3)" strokeWidth="1.5" />

        {/* Kính lái phản chiếu */}
        <path d="M 124 26 L 140 34 L 148 50 L 122 50 Z" fill="url(#glassGrad)" />
        <path d="M 74 30 L 118 30 L 118 50 L 60 50 Z" fill="url(#glassGrad)" />

        {/* Đèn LED phát sáng */}
        <ellipse cx="174" cy="62" rx="4" ry="7" fill="url(#glowHeadlight)" className="light-glow-anim" />
        <ellipse cx="26" cy="62" rx="3" ry="6" fill="#ef4444" opacity="0.8" />

        <line x1="60" y1="52" x2="148" y2="52" stroke="rgba(255,255,255,0.4)" strokeWidth="1" />
      </svg>
    </div>
  )
}

export default function App() {
  const [slots, setSlots] = useState([
    { id: 1, occupied: false, rfid: '', time_in: '', duration_sec: 0, current_fee: 0, client_ip: '', client_port: 0 },
    { id: 2, occupied: false, rfid: '', time_in: '', duration_sec: 0, current_fee: 0, client_ip: '', client_port: 0 },
    { id: 3, occupied: false, rfid: '', time_in: '', duration_sec: 0, current_fee: 0, client_ip: '', client_port: 0 },
  ])
  const [log, setLog] = useState([])
  const [stats, setStats] = useState({
    total_in: 0, total_out: 0, revenue: 0,
    active_clients: 0, occupancy: 0, capacity: 3,
    server_time: '', rate_per_minute: 5000
  })
  const [connMode, setConnMode] = useState('disconnected') // 'websocket' | 'polling' | 'disconnected'
  const [lastUpdate, setLastUpdate] = useState('')
  const [filterType, setFilterType] = useState('all')

  const wsRef = useRef(null)
  const pollingRef = useRef(null)

  const updateState = useCallback((data) => {
    if (data.slots) setSlots(data.slots)
    if (data.log) setLog(data.log)
    if (data.stats) setStats(data.stats)
    setLastUpdate(new Date().toLocaleTimeString('vi-VN'))
  }, [])

  // HTTP Polling fallback
  const startPolling = useCallback(() => {
    if (pollingRef.current) return
    pollingRef.current = setInterval(async () => {
      try {
        const res = await fetch(HTTP_URL)
        const data = await res.json()
        updateState(data)
        setConnMode('polling')
      } catch {
        setConnMode('disconnected')
      }
    }, 1000)
  }, [updateState])

  // WebSocket kết nối thời gian thực
  useEffect(() => {
    function connectWS() {
      const ws = new WebSocket(WS_URL)

      ws.onopen = () => {
        setConnMode('websocket')
        if (pollingRef.current) {
          clearInterval(pollingRef.current)
          pollingRef.current = null
        }
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          updateState(data)
        } catch (e) {
          console.error(e)
        }
      }

      ws.onerror = () => {
        startPolling()
      }

      ws.onclose = () => {
        wsRef.current = null
        startPolling()
        setTimeout(connectWS, 4000)
      }

      wsRef.current = ws
    }

    connectWS()

    return () => {
      if (wsRef.current) wsRef.current.close()
      if (pollingRef.current) clearInterval(pollingRef.current)
    }
  }, [updateState, startPolling])

  const occupiedCount = slots.filter(s => s.occupied).length
  const freeCount = slots.length - occupiedCount

  const filteredLog = log.filter(item => {
    if (filterType === 'all') return true
    return item.type === filterType
  })

  return (
    <div className="cyber-app-wrapper">
      {/* Lưới viễn cảnh Cyber 3D nền */}
      <div className="perspective-grid-bg"></div>
      <div className="cyber-glow-orb orb-cyan"></div>
      <div className="cyber-glow-orb orb-purple"></div>

      <div className="main-content-container">
        {/* Header */}
        <header className="cyber-header">
          <div className="brand-group">
            <div className="hologram-logo">
              <span className="logo-symbol">🅿️</span>
              <div className="hologram-ring"></div>
            </div>
            <div>
              <div className="sub-dept">VKU ĐÀ NẴNG </div>
              <h1 className="cyber-title">HỆ THỐNG QUẢN LÝ BÃI ĐỖ XE THÔNG MINH</h1>
            </div>
          </div>

          <div className="header-meta-group">
            <div className={`cyber-status-tag ${connMode}`}>
              <span className="blinking-dot"></span>
              <span className="status-text">
                {connMode === 'websocket' && 'WEBSOCKET: LIVE'}
                {connMode === 'polling' && 'HTTP POLLING '}
                {connMode === 'disconnected' && 'OFFLINE '}
              </span>
            </div>
            <div className="clock-tag">SYNC: {lastUpdate || '--:--:--'}</div>
          </div>
        </header>

        {/* 6 Khối Thống Kê Chỉ Số */}
        <section className="metrics-grid">
          <div className="metric-box card-capacity">
            <div className="metric-icon">🏢</div>
            <div className="metric-content">
              <div className="metric-label">TỔNG SỨC CHỨA</div>
              <div className="metric-value">{stats.capacity} <span className="unit">Ô</span></div>
            </div>
          </div>

          <div className="metric-box card-occupied">
            <div className="metric-icon">🚘</div>
            <div className="metric-content">
              <div className="metric-label">XE ĐANG ĐỖ</div>
              <div className="metric-value text-red">{occupiedCount} <span className="unit">Xe</span></div>
            </div>
          </div>

          <div className="metric-box card-free">
            <div className="metric-icon">🟢</div>
            <div className="metric-content">
              <div className="metric-label">CHỖ TRỐNG</div>
              <div className="metric-value text-green">{freeCount} <span className="unit">Chỗ</span></div>
            </div>
          </div>

          <div className="metric-box card-in">
            <div className="metric-icon">📥</div>
            <div className="metric-content">
              <div className="metric-label">LƯỢT XE VÀO</div>
              <div className="metric-value">{stats.total_in}</div>
            </div>
          </div>

          <div className="metric-box card-out">
            <div className="metric-icon">📤</div>
            <div className="metric-content">
              <div className="metric-label">LƯỢT XE RA</div>
              <div className="metric-value">{stats.total_out}</div>
            </div>
          </div>

          <div className="metric-box card-revenue">
            <div className="metric-icon">💰</div>
            <div className="metric-content">
              <div className="metric-label">TỔNG DOANH THU</div>
              <div className="metric-value text-gold">
                {(stats.revenue || 0).toLocaleString('vi-VN')}
                <span className="unit text-gold">₫</span>
              </div>
            </div>
          </div>
        </section>

        {/* KHÔNG GIAN 3 Ô ĐỖ XE 3D */}
        <section className="parking-stage-section">
          <div className="stage-header">
            <div>
              <h2 className="stage-title">📍 KHÔNG GIAN BÃI ĐỖ XE 3D (THỜI GIAN THỰC)</h2>
              <p className="stage-subtitle">Giám sát phân tán TCP Socket 8888 · Biểu phí gửi xe: 5.000₫/phút</p>
            </div>
            <div className="sensor-live-pill">
              <span className="pulse-sensor"></span> SENSOR ACTIVE
            </div>
          </div>

          <div className="isometric-stage-grid">
            {slots.map((slot) => {
              const carColor = SLOT_CAR_COLORS[slot.id] || '#38bdf8'
              const durMin = Math.floor((slot.duration_sec || 0) / 60)
              const durSec = (slot.duration_sec || 0) % 60

              return (
                <div 
                  key={slot.id} 
                  className={`bay-3d-card ${slot.occupied ? 'occupied-active' : 'free-active'}`}
                >
                  {/* Tiêu đề ô */}
                  <div className="bay-badge-bar">
                    <span className="bay-id-tag">Ô ĐỖ 0{slot.id}</span>
                    <span className={`status-pill-neon ${slot.occupied ? 'pill-danger' : 'pill-success'}`}>
                      {slot.occupied ? '● ĐANG CÓ XE' : '● ĐANG TRỐNG'}
                    </span>
                  </div>

                  {/* Mặt sàn 3D */}
                  <div className="isometric-ground-plate">
                    <div className="bay-grid-stripes"></div>

                    {slot.occupied ? (
                      /* XE 3D NỔI & ĐÈN PHA */
                      <div className="car-parking-scene">
                        <IsometricCar color={carColor} />
                        <div className="plate-tag-3d">
                          <span className="plate-text">THẺ RFID: {slot.rfid}</span>
                        </div>
                        <div className="vehicle-model-caption">Khách Vãng Lai</div>
                      </div>
                    ) : (
                      /* Ô TRỐNG PHÁT SÁNG HOLOGRAPHIC */
                      <div className="empty-slot-scene">
                        <div className="holo-beacon">
                          <span className="holo-p">P</span>
                          <div className="holo-rings"></div>
                        </div>
                        <div className="empty-slot-msg">VỊ TRÍ SẴN SÀNG</div>
                        <div className="empty-slot-sub">Chờ quẹt thẻ vào bãi</div>
                      </div>
                    )}
                  </div>

                  {/* Thông số ô đỗ */}
                  {slot.occupied ? (
                    <div className="bay-info-panel">
                      <div className="info-row">
                        <span className="lbl">Mã thẻ RFID:</span>
                        <span className="val rfid-tag">{slot.rfid}</span>
                      </div>
                      <div className="info-row">
                        <span className="lbl">Đối tượng:</span>
                        <span className="val">Khách Vãng Lai</span>
                      </div>
                      <div className="info-row">
                        <span className="lbl">Giờ xe vào:</span>
                        <span className="val font-mono">{slot.time_in || '--:--:--'}</span>
                      </div>
                      <div className="info-row">
                        <span className="lbl">Thời gian đỗ:</span>
                        <span className="val font-mono">{durMin} phút {durSec} giây</span>
                      </div>
                      <div className="info-row price-row">
                        <span className="lbl">Phí tạm tính:</span>
                        <span className="val fee-gold-val font-mono">
                          {(slot.current_fee || 5000).toLocaleString('vi-VN')} VND
                        </span>
                      </div>
                      <div className="socket-trace-tag">
                        📡 Client Socket: {slot.client_ip || '127.0.0.1'}:{slot.client_port || '8888'}
                      </div>
                    </div>
                  ) : (
                    <div className="bay-info-panel bay-empty-panel">
                      <span className="empty-hint">Vị trí trống. Quẹt thẻ tại cổng vào để đỗ xe.</span>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </section>

        {/* BẢNG LỊCH SỬ RA VÀO CÓ THANH CUỘN */}
        <section className="log-panel-section">
          <div className="log-panel-header">
            <div>
              <h2 className="panel-title">📋 NHẬT KÝ GIAO DỊCH & RA VÀO (TRANSACTION LOG)</h2>
              <p className="panel-sub">Tự động lưu trữ bền vững vào <code>parking_data.json</code></p>
            </div>

            <div className="filter-pill-group">
              <button 
                className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
                onClick={() => setFilterType('all')}
              >
                Tất Cả ({log.length})
              </button>
              <button 
                className={`filter-btn ${filterType === 'entry' ? 'active' : ''}`}
                onClick={() => setFilterType('entry')}
              >
                Xe Vào
              </button>
              <button 
                className={`filter-btn ${filterType === 'exit' ? 'active' : ''}`}
                onClick={() => setFilterType('exit')}
              >
                Xe Ra
              </button>
              <button 
                className={`filter-btn ${filterType === 'reject' ? 'active' : ''}`}
                onClick={() => setFilterType('reject')}
              >
                Từ Chối
              </button>
            </div>
          </div>

          {filteredLog.length === 0 ? (
            <div className="empty-log-state">
              <div className="empty-icon">📭</div>
              <div>Chưa có nhật ký giao dịch. Hãy quẹt thẻ từ Client để phát sinh dữ liệu!</div>
            </div>
          ) : (
            <div className="cyber-table-container">
              <table className="cyber-table">
                <thead>
                  <tr>
                    <th>THỜI GIAN</th>
                    <th>CHI TIẾT SỰ KIỆN</th>
                    <th>SOCKET CLIENT (IP:PORT)</th>
                    <th>PHÍ GIAO DỊCH</th>
                    <th>TRẠNG THÁI</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLog.map((entry, idx) => (
                    <tr key={idx} className={idx === 0 ? 'row-glow-flash' : ''}>
                      <td className="font-mono text-muted">{entry.time}</td>
                      <td className="event-cell">{entry.event}</td>
                      <td className="font-mono ip-cell">{entry.client_ip || '127.0.0.1:8888'}</td>
                      <td className="fee-cell font-mono">
                        {entry.fee > 0 ? (
                          <span className="fee-paid-badge">+{entry.fee.toLocaleString('vi-VN')} VND</span>
                        ) : (
                          <span className="text-dark">--</span>
                        )}
                      </td>
                      <td>
                        {entry.type === 'entry' && <span className="chip chip-in">⬆️ VÀO BÃI</span>}
                        {entry.type === 'exit' && <span className="chip chip-out">⬇️ RA BÃI</span>}
                        {entry.type === 'reject' && <span className="chip chip-reject">🚫 TỪ CHỐI</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Footer */}
        <footer className="cyber-footer">
          <div>🅿️  BÃI ĐỖ XE THÔNG MINH · VKU ĐÀ NẴNG</div>  
        </footer>
      </div>
    </div>
  )
}
