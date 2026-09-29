import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;

public class ParkingMonitor extends JFrame {

    // Mau sac Cyberpunk Dark Theme
    private static final Color BG_DARK        = new Color(11, 15, 25);
    private static final Color CARD_BG        = new Color(19, 28, 46);
    private static final Color CARD_BORDER    = new Color(40, 53, 79);
    private static final Color TEXT_WHITE      = new Color(248, 250, 252);
    private static final Color TEXT_MUTED      = new Color(148, 163, 184);
    private static final Color NEON_CYAN       = new Color(6, 182, 212);
    private static final Color NEON_GREEN      = new Color(16, 185, 129);
    private static final Color NEON_RED        = new Color(239, 68, 68);
    private static final Color NEON_GOLD       = new Color(245, 158, 11);

    // Font chu
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BIG_NUM = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MONO    = new Font("Consolas", Font.BOLD, 12);

    // Metrics components
    private JLabel lblCapacity, lblOccupied, lblFree, lblTotalIn, lblTotalOut, lblRevenue;
    private JLabel lblServerIp, lblClientList;
    private BayPanel[] bayPanels = new BayPanel[3];
    private DefaultTableModel logTableModel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String STATUS_URL = "http://127.0.0.1:9999/status";

    public ParkingMonitor() {
        setTitle("HỆ THỐNG QUẢN LÝ BÃI ĐỖ XE THÔNG MINH - VKU ĐÀ NẴNG (DESKTOP MONITOR)");
        setSize(1200, 850);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(15, 15));

        // 1. TOP PANEL: Title & Network IP Bar
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL: Metrics + 3 Parking Bays
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(new EmptyBorder(0, 18, 0, 18));

        centerPanel.add(createMetricsPanel());
        centerPanel.add(Box.createVerticalStrut(14));
        centerPanel.add(createBaysSection());
        add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM PANEL: Transaction Log Table
        JPanel bottomPanel = createLogPanel();
        bottomPanel.setPreferredSize(new Dimension(1160, 240));
        bottomPanel.setBorder(new EmptyBorder(0, 18, 15, 18));
        add(bottomPanel, BorderLayout.SOUTH);

        // Timer dinh ky cap nhat 500ms
        Timer timer = new Timer(500, e -> fetchData());
        timer.start();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CARD_BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Logo & Title
        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleGroup.setBackground(CARD_BG);

        JLabel logo = new JLabel("🅿️");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        titleGroup.add(logo);

        JPanel titleText = new JPanel();
        titleText.setLayout(new BoxLayout(titleText, BoxLayout.Y_AXIS));
        titleText.setBackground(CARD_BG);

        JLabel sub = new JLabel("VKU ĐÀ NẴNG · BÁO CÁO LẬP TRÌNH MẠNG (DESKTOP GUI)");
        sub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sub.setForeground(NEON_CYAN);
        titleText.add(sub);

        JLabel mainTitle = new JLabel("HỆ THỐNG QUẢN LÝ BÃI ĐỖ XE THÔNG MINH");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(TEXT_WHITE);
        titleText.add(mainTitle);

        titleGroup.add(titleText);
        panel.add(titleGroup, BorderLayout.WEST);

        // Network Socket IP Bar (SERVER IP & CLIENT IP)
        JPanel netGroup = new JPanel(new GridLayout(1, 2, 14, 0));
        netGroup.setBackground(CARD_BG);

        // Server IP Box
        JPanel serverBox = new JPanel(new BorderLayout(6, 2));
        serverBox.setBackground(new Color(11, 15, 25));
        serverBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(16, 185, 129, 100), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        JLabel lblSrvRole = new JLabel("MÁY CHỦ (SERVER TCP : PORT)");
        lblSrvRole.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblSrvRole.setForeground(TEXT_MUTED);
        lblServerIp = new JLabel("10.187.149.136:8888");
        lblServerIp.setFont(FONT_MONO);
        lblServerIp.setForeground(NEON_GREEN);
        serverBox.add(lblSrvRole, BorderLayout.NORTH);
        serverBox.add(lblServerIp, BorderLayout.CENTER);
        netGroup.add(serverBox);

        // Client IP Box
        JPanel clientBox = new JPanel(new BorderLayout(6, 2));
        clientBox.setBackground(new Color(11, 15, 25));
        clientBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(6, 182, 212, 100), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        JLabel lblCliRole = new JLabel("MÁY TRẠM (CLIENT SOCKET)");
        lblCliRole.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblCliRole.setForeground(TEXT_MUTED);
        lblClientList = new JLabel("Chờ Client kết nối...");
        lblClientList.setFont(FONT_MONO);
        lblClientList.setForeground(NEON_CYAN);
        clientBox.add(lblCliRole, BorderLayout.NORTH);
        clientBox.add(lblClientList, BorderLayout.CENTER);
        netGroup.add(clientBox);

        panel.add(netGroup, BorderLayout.EAST);
        return panel;
    }

    private JPanel createMetricsPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 6, 12, 0));
        grid.setBackground(BG_DARK);
        grid.setMaximumSize(new Dimension(2000, 75));

        lblCapacity = new JLabel("3");
        lblOccupied = new JLabel("0");
        lblFree     = new JLabel("3");
        lblTotalIn  = new JLabel("0");
        lblTotalOut = new JLabel("0");
        lblRevenue  = new JLabel("0 ₫");

        grid.add(createMetricCard("SỨC CHỨA", lblCapacity, TEXT_WHITE, "🏢"));
        grid.add(createMetricCard("XE ĐANG ĐỖ", lblOccupied, NEON_RED, "🚘"));
        grid.add(createMetricCard("CÒN TRỐNG", lblFree, NEON_GREEN, "🟢"));
        grid.add(createMetricCard("TỔNG VÀO", lblTotalIn, TEXT_WHITE, "📥"));
        grid.add(createMetricCard("TỔNG RA", lblTotalOut, TEXT_WHITE, "📤"));
        grid.add(createMetricCard("DOANH THU", lblRevenue, NEON_GOLD, "💰"));

        return grid;
    }

    private JPanel createMetricCard(String title, JLabel valLabel, Color valColor, String icon) {
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel(icon + " " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(TEXT_MUTED);

        valLabel.setFont(FONT_BIG_NUM);
        valLabel.setForeground(valColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBaysSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_DARK);

        JLabel sectionTitle = new JLabel("📍 TRẠNG THÁI 3 Ô ĐỖ XE THỜI GIAN THỰC");
        sectionTitle.setFont(FONT_HEADER);
        sectionTitle.setForeground(TEXT_WHITE);
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel baysGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        baysGrid.setBackground(BG_DARK);
        for (int i = 0; i < 3; i++) {
            bayPanels[i] = new BayPanel(i + 1);
            baysGrid.add(bayPanels[i]);
        }
        panel.add(baysGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(BG_DARK);

        JLabel title = new JLabel("📋 NHẬT KÝ GIAO DỊCH MẠNG (TRANSACTION LOG)");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_WHITE);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"THỜI GIAN", "CHI TIẾT SỰ KIỆN", "SOCKET CLIENT (IP:PORT)", "PHÍ (VND)", "LOẠI"};
        logTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(logTableModel);
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_WHITE);
        table.setFont(FONT_REGULAR);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setBackground(new Color(11, 15, 25));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        // Can giua / dep bang
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(CARD_BG);
        scrollPane.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // Lay du lieu tu C++ Server qua REST API (Port 9999)
    private void fetchData() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(STATUS_URL)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                parseAndUpdate(resp.body());
            }
        } catch (Exception ignored) {
            // Server chua bat
        }
    }

    private void parseAndUpdate(String json) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Update 3 bays
                for (int i = 1; i <= 3; i++) {
                    int slotIdx = json.indexOf("{\"id\":" + i);
                    if (slotIdx != -1) {
                        int endIdx = json.indexOf("}", slotIdx);
                        String slotData = json.substring(slotIdx, endIdx + 1);

                        boolean occupied = slotData.contains("\"occupied\":true");
                        String rfid = extractStr(slotData, "rfid");
                        String timeIn = extractStr(slotData, "time_in");
                        long fee = extractNum(slotData, "current_fee");
                        long dur = extractNum(slotData, "duration_sec");
                        String ip = extractStr(slotData, "client_ip");
                        long port = extractNum(slotData, "client_port");

                        bayPanels[i - 1].updateState(occupied, rfid, timeIn, dur, fee, ip, port);
                    }
                }

                // Update Stats
                int statsIdx = json.indexOf("\"stats\":{");
                if (statsIdx != -1) {
                    String statsStr = json.substring(statsIdx);
                    long totalIn  = extractNum(statsStr, "total_in");
                    long totalOut = extractNum(statsStr, "total_out");
                    long revenue  = extractNum(statsStr, "revenue");
                    long occ      = extractNum(statsStr, "occupancy");
                    long clients  = extractNum(statsStr, "active_clients");

                    lblCapacity.setText("3 Ô");
                    lblOccupied.setText(occ + " Xe");
                    lblFree.setText((3 - occ) + " Chỗ");
                    lblTotalIn.setText(String.valueOf(totalIn));
                    lblTotalOut.setText(String.valueOf(totalOut));
                    lblRevenue.setText(NumberFormat.getNumberInstance(Locale.GERMANY).format(revenue) + " ₫");

                    // Connected clients list
                    if (clients > 0) {
                        lblClientList.setText(clients + " Trạm đang Online");
                        lblClientList.setForeground(NEON_GREEN);
                    } else {
                        lblClientList.setText("Chờ Client kết nối...");
                        lblClientList.setForeground(TEXT_MUTED);
                    }
                }

                // Update Log
                int logIdx = json.indexOf("\"log\":[");
                if (logIdx != -1) {
                    int logEnd = json.indexOf("],\"clients\":", logIdx);
                    if (logEnd == -1) logEnd = json.indexOf("],\"stats\":", logIdx);
                    if (logEnd != -1) {
                        String logJson = json.substring(logIdx + 7, logEnd);
                        logTableModel.setRowCount(0);
                        String[] entries = logJson.split("\\},\\{");
                        for (String entry : entries) {
                            if (entry.isEmpty()) continue;
                            String t    = extractStr(entry, "time");
                            String ev   = extractStr(entry, "event");
                            String ip   = extractStr(entry, "client_ip");
                            long f      = extractNum(entry, "fee");
                            String type = extractStr(entry, "type");
                            String typeStr = "entry".equals(type) ? "⬆️ VÀO" : ("exit".equals(type) ? "⬇️ RA" : "🚫 TỪ CHỐI");
                            String feeStr  = f > 0 ? NumberFormat.getNumberInstance(Locale.GERMANY).format(f) + " ₫" : "--";
                            logTableModel.addRow(new Object[]{t, ev, ip, feeStr, typeStr});
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private String extractStr(String json, String key) {
        String k = "\"" + key + "\":\"";
        int s = json.indexOf(k);
        if (s == -1) return "";
        s += k.length();
        int e = json.indexOf("\"", s);
        return (e != -1) ? json.substring(s, e) : "";
    }

    private long extractNum(String json, String key) {
        String k = "\"" + key + "\":";
        int s = json.indexOf(k);
        if (s == -1) return 0;
        s += k.length();
        int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++;
        try {
            return Long.parseLong(json.substring(s, e));
        } catch (Exception ex) {
            return 0;
        }
    }

    // =========================================================================
    //  BAY PANEL (VE XE 3D + THONG SO)
    // =========================================================================
    static class BayPanel extends JPanel {
        private final int bayId;
        private boolean occupied = false;
        private String rfid = "";
        private String timeIn = "";
        private long durationSec = 0;
        private long fee = 0;
        private String clientAddr = "";

        private JLabel lblBayTitle;
        private JLabel lblStatusBadge;
        private CarCanvas carCanvas;
        private JLabel lblRfid, lblTime, lblDur, lblFee, lblSocket;

        public BayPanel(int id) {
            this.bayId = id;
            setLayout(new BorderLayout(8, 8));
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CARD_BORDER, 1),
                    new EmptyBorder(12, 14, 12, 14)
            ));

            // Top
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(CARD_BG);
            lblBayTitle = new JLabel("Ô ĐỖ 0" + bayId);
            lblBayTitle.setFont(FONT_HEADER);
            lblBayTitle.setForeground(TEXT_WHITE);

            lblStatusBadge = new JLabel("● ĐANG TRỐNG");
            lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblStatusBadge.setForeground(NEON_GREEN);

            top.add(lblBayTitle, BorderLayout.WEST);
            top.add(lblStatusBadge, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Center Visual
            carCanvas = new CarCanvas();
            carCanvas.setPreferredSize(new Dimension(280, 110));
            add(carCanvas, BorderLayout.CENTER);

            // Info panel
            JPanel info = new JPanel(new GridLayout(5, 1, 2, 4));
            info.setBackground(CARD_BG);
            info.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER),
                    new EmptyBorder(8, 0, 0, 0)
            ));

            lblRfid = createRow(info, "Mã RFID:", "--");
            lblTime = createRow(info, "Giờ vào:", "--");
            lblDur  = createRow(info, "Thời gian:", "--");
            lblFee  = createRow(info, "Phí tạm tính:", "--");
            lblFee.setForeground(NEON_GOLD);
            lblSocket = createRow(info, "Socket:", "--");
            lblSocket.setFont(FONT_MONO);

            add(info, BorderLayout.SOUTH);
        }

        private JLabel createRow(JPanel parent, String label, String val) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(CARD_BG);
            JLabel l = new JLabel(label);
            l.setFont(FONT_REGULAR);
            l.setForeground(TEXT_MUTED);
            JLabel v = new JLabel(val);
            v.setFont(FONT_REGULAR);
            v.setForeground(TEXT_WHITE);
            p.add(l, BorderLayout.WEST);
            p.add(v, BorderLayout.EAST);
            parent.add(p);
            return v;
        }

        public void updateState(boolean occ, String rfid, String timeIn, long dur, long fee, String ip, long port) {
            this.occupied = occ;
            this.rfid = rfid;
            this.timeIn = timeIn;
            this.durationSec = dur;
            this.fee = fee;
            this.clientAddr = (!ip.isEmpty()) ? ip + ":" + port : "--";

            if (occupied) {
                lblStatusBadge.setText("● CÓ XE ĐỖ");
                lblStatusBadge.setForeground(NEON_RED);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(239, 68, 68, 120), 1),
                        new EmptyBorder(12, 14, 12, 14)
                ));

                lblRfid.setText(rfid);
                lblTime.setText(timeIn);
                lblDur.setText((dur / 60) + "p " + (dur % 60) + "s");
                lblFee.setText(NumberFormat.getNumberInstance(Locale.GERMANY).format(fee) + " VND");
                lblSocket.setText(clientAddr);
            } else {
                lblStatusBadge.setText("● ĐANG TRỐNG");
                lblStatusBadge.setForeground(NEON_GREEN);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CARD_BORDER, 1),
                        new EmptyBorder(12, 14, 12, 14)
                ));

                lblRfid.setText("--");
                lblTime.setText("--");
                lblDur.setText("--");
                lblFee.setText("--");
                lblSocket.setText("--");
            }
            carCanvas.repaint();
        }

        class CarCanvas extends JPanel {
            public CarCanvas() {
                setBackground(new Color(11, 15, 25));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Mat san bai do
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawRoundRect(15, 10, w - 30, h - 20, 10, 10);

                if (occupied) {
                    // Ve xe the thao Cyberpunk
                    int cx = w / 2 - 50;
                    int cy = h / 2 - 20;

                    // Den pha chieu sang
                    GradientPaint beam = new GradientPaint(cx + 90, cy + 15, new Color(254, 240, 138, 120),
                            cx + 140, cy + 15, new Color(6, 182, 212, 0));
                    g2.setPaint(beam);
                    g2.fillPolygon(new int[]{cx + 90, cx + 140, cx + 140, cx + 90},
                            new int[]{cy + 10, cy, cy + 30, cy + 20}, 4);

                    // Banh xe
                    g2.setColor(new Color(2, 6, 23));
                    g2.fillOval(cx + 10, cy + 25, 18, 18);
                    g2.fillOval(cx + 70, cy + 25, 18, 18);
                    g2.setColor(TEXT_MUTED);
                    g2.drawOval(cx + 10, cy + 25, 18, 18);
                    g2.drawOval(cx + 70, cy + 25, 18, 18);

                    // Than xe
                    Color carColor = (bayId == 1) ? new Color(56, 189, 248) :
                            (bayId == 2 ? new Color(244, 63, 94) : new Color(245, 158, 11));
                    g2.setColor(carColor);
                    g2.fillRoundRect(cx, cy + 10, 100, 20, 12, 12);

                    // Cabin & Kinh
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(cx + 25, cy - 2, 45, 16, 8, 8);
                    g2.setColor(NEON_CYAN);
                    g2.fillRoundRect(cx + 45, cy + 1, 20, 10, 4, 4);

                    // Den pha LED
                    g2.setColor(new Color(254, 240, 138));
                    g2.fillOval(cx + 96, cy + 14, 5, 8);

                    // Bien so the RFID
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(cx + 15, cy + 33, 70, 14, 4, 4);
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.drawString(rfid, cx + 20, cy + 44);
                } else {
                    // Hologram chu P
                    g2.setColor(new Color(16, 185, 129, 60));
                    g2.fillOval(w / 2 - 25, h / 2 - 25, 50, 50);
                    g2.setColor(NEON_GREEN);
                    g2.drawOval(w / 2 - 25, h / 2 - 25, 50, 50);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                    g2.drawString("P", w / 2 - 8, h / 2 + 8);
                }
                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ParkingMonitor().setVisible(true);
        });
    }
}
