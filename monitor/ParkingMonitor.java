import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Enumeration;
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

    // Font chu chuan he thong Windows khong bi loi o vuong
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 17);
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

    private static String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                if (!net.isUp() || net.isLoopback() || net.isVirtual()) continue;
                String name = (net.getName() + " " + net.getDisplayName()).toLowerCase();
                if (name.contains("vmware") || name.contains("virtual") || name.contains("vbox") || name.contains("wsl")) continue;
                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        String host = a.getHostAddress();
                        if (!host.startsWith("169.254")) return host;
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {}
        return "10.187.149.136";
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CARD_BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Title
        JPanel titleText = new JPanel();
        titleText.setLayout(new BoxLayout(titleText, BoxLayout.Y_AXIS));
        titleText.setBackground(CARD_BG);

        JLabel sub = new JLabel("VKU ĐÀ NẴNG · BÁO CÁO LẬP TRÌNH MẠNG (DESKTOP GUI)");
        sub.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sub.setForeground(NEON_CYAN);
        titleText.add(sub);

        JLabel mainTitle = new JLabel("HỆ THỐNG QUẢN LÝ BÃI ĐỖ XE THÔNG MINH");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(TEXT_WHITE);
        titleText.add(mainTitle);

        panel.add(titleText, BorderLayout.WEST);

        // Network Socket IP Bar (SERVER IP & CLIENT IP)
        JPanel netGroup = new JPanel(new GridLayout(1, 2, 14, 0));
        netGroup.setBackground(CARD_BG);

        // Server IP Box
        JPanel serverBox = new JPanel(new BorderLayout(6, 2));
        serverBox.setBackground(new Color(11, 15, 25));
        serverBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(16, 185, 129, 120), 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        JLabel lblSrvRole = new JLabel("MÁY CHỦ (SERVER TCP : PORT)");
        lblSrvRole.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblSrvRole.setForeground(TEXT_MUTED);
        lblServerIp = new JLabel(detectLocalIp() + ":8888");
        lblServerIp.setFont(new Font("Consolas", Font.BOLD, 13));
        lblServerIp.setForeground(NEON_GREEN);
        serverBox.add(lblSrvRole, BorderLayout.NORTH);
        serverBox.add(lblServerIp, BorderLayout.CENTER);
        netGroup.add(serverBox);

        // Client IP Box
        JPanel clientBox = new JPanel(new BorderLayout(6, 2));
        clientBox.setBackground(new Color(11, 15, 25));
        clientBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(6, 182, 212, 120), 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        JLabel lblCliRole = new JLabel("MÁY TRẠM (CLIENT SOCKET)");
        lblCliRole.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCliRole.setForeground(TEXT_MUTED);
        lblClientList = new JLabel("Chờ Client kết nối...");
        lblClientList.setFont(new Font("Consolas", Font.BOLD, 13));
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

        grid.add(createMetricCard("SỨC CHỨA", lblCapacity, TEXT_WHITE));
        grid.add(createMetricCard("XE ĐANG ĐỖ", lblOccupied, NEON_RED));
        grid.add(createMetricCard("CÒN TRỐNG", lblFree, NEON_GREEN));
        grid.add(createMetricCard("TỔNG VÀO", lblTotalIn, TEXT_WHITE));
        grid.add(createMetricCard("TỔNG RA", lblTotalOut, TEXT_WHITE));
        grid.add(createMetricCard("DOANH THU", lblRevenue, NEON_GOLD));

        return grid;
    }

    private JPanel createMetricCard(String title, JLabel valLabel, Color valColor) {
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(TEXT_MUTED);

        valLabel.setFont(FONT_BIG_NUM);
        valLabel.setForeground(valColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBaysSection() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setBackground(BG_DARK);

        for (int i = 0; i < 3; i++) {
            bayPanels[i] = new BayPanel(i + 1);
            panel.add(bayPanels[i]);
        }
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Header Panel
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(CARD_BG);

        JLabel title = new JLabel("LỊCH SỬ GIAO DỊCH VÀO / RA TRẠM ĐỖ (TRANSACTION LOGS)");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_WHITE);

        JLabel liveBadge = new JLabel("● CẬP NHẬT TRỰC TIẾP");
        liveBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        liveBadge.setForeground(NEON_GREEN);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(liveBadge, BorderLayout.EAST);
        panel.add(hdr, BorderLayout.NORTH);

        // Table
        String[] cols = {"THỜI GIAN", "SỰ KIỆN GIAO DỊCH", "ĐỊA CHỈ IP SOCKET", "CƯỚC PHÍ", "TRẠNG THÁI"};
        logTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(logTableModel);
        table.setBackground(new Color(11, 15, 25));
        table.setForeground(TEXT_WHITE);
        table.setGridColor(new Color(30, 41, 59));
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setBackground(new Color(15, 23, 42));
        table.getTableHeader().setForeground(NEON_CYAN);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(CARD_BORDER));

        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        // Custom Renderer cho Cot Trang Thai
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSelected, hasFocus, r, c);
                l.setHorizontalAlignment(JLabel.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String v = (val != null) ? val.toString() : "";
                if ("VÀO".equals(v)) {
                    l.setForeground(NEON_GREEN);
                } else if ("RA".equals(v)) {
                    l.setForeground(NEON_CYAN);
                } else {
                    l.setForeground(NEON_RED);
                }
                l.setBackground(r % 2 == 0 ? new Color(11, 15, 25) : new Color(15, 23, 42));
                return l;
            }
        });

        // Renderer cho Cot Phi
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSelected, hasFocus, r, c);
                l.setHorizontalAlignment(JLabel.RIGHT);
                l.setFont(FONT_MONO);
                l.setForeground(NEON_GOLD);
                l.setBackground(r % 2 == 0 ? new Color(11, 15, 25) : new Color(15, 23, 42));
                return l;
            }
        });

        // Renderer cho Cot Socket IP
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSelected, hasFocus, r, c);
                l.setHorizontalAlignment(JLabel.CENTER);
                l.setFont(FONT_MONO);
                l.setForeground(new Color(56, 189, 248));
                l.setBackground(r % 2 == 0 ? new Color(11, 15, 25) : new Color(15, 23, 42));
                return l;
            }
        });

        // Renderer cho Thoi gian
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(11, 15, 25));
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
        } catch (Exception ignored) {}
    }

    private void parseAndUpdate(String json) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Update 3 Bays
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

                // 2. Update Metrics
                int statsIdx = json.indexOf("\"stats\":{");
                if (statsIdx != -1) {
                    String statsStr = json.substring(statsIdx);
                    long totalIn  = extractNum(statsStr, "total_in");
                    long totalOut = extractNum(statsStr, "total_out");
                    long revenue  = extractNum(statsStr, "revenue");
                    long occ      = extractNum(statsStr, "occupancy");

                    lblCapacity.setText("3 Ô");
                    lblOccupied.setText(occ + " Xe");
                    lblFree.setText((3 - occ) + " Chỗ");
                    lblTotalIn.setText(String.valueOf(totalIn));
                    lblTotalOut.setText(String.valueOf(totalOut));
                    lblRevenue.setText(NumberFormat.getNumberInstance(Locale.GERMANY).format(revenue) + " ₫");
                }

                // 3. Update Log Table
                String lastLogIp = "";
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
                            String typeStr = "entry".equals(type) ? "VÀO" : ("exit".equals(type) ? "RA" : "TỪ CHỐI");
                            String feeStr  = f > 0 ? NumberFormat.getNumberInstance(Locale.GERMANY).format(f) + " ₫" : "--";
                            logTableModel.addRow(new Object[]{t, ev, ip, feeStr, typeStr});
                            if (lastLogIp.isEmpty() && ip != null && !ip.isEmpty() && !"-".equals(ip)) {
                                lastLogIp = ip;
                            }
                        }
                    }
                }

                // 4. Update Connected Clients list
                int clientsIdx = json.indexOf("\"clients\":[");
                boolean foundClients = false;
                if (clientsIdx != -1) {
                    int arrStart = clientsIdx + 11;
                    int clientsEnd = json.indexOf("]", arrStart);
                    if (clientsEnd > arrStart) {
                        String clientsJson = json.substring(arrStart, clientsEnd);
                        StringBuilder sb = new StringBuilder();
                        int p = 0;
                        while ((p = clientsJson.indexOf("\"addr\":\"", p)) != -1) {
                            p += 8;
                            int endQ = clientsJson.indexOf("\"", p);
                            if (endQ != -1) {
                                String addr = clientsJson.substring(p, endQ);
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(addr);
                                p = endQ + 1;
                            }
                        }
                        if (sb.length() > 0) {
                            lblClientList.setText(sb.toString() + " (Online)");
                            lblClientList.setForeground(NEON_GREEN);
                            foundClients = true;
                        }
                    }
                }

                // Neu chua co client online, lay IP tu log gan nhat
                if (!foundClients) {
                    if (!lastLogIp.isEmpty()) {
                        lblClientList.setText(lastLogIp + " (Gần nhất)");
                        lblClientList.setForeground(TEXT_MUTED);
                    } else {
                        lblClientList.setText("Chờ Client kết nối...");
                        lblClientList.setForeground(TEXT_MUTED);
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
    //  BAY PANEL (VE XE + THONG SO)
    // =========================================================================
    class BayPanel extends JPanel {
        private final int bayId;
        private boolean occupied = false;
        private String rfid = "--";
        private String timeIn = "--";
        private long durationSec = 0;
        private long fee = 0;
        private String clientAddr = "--";

        private JLabel lblStatusBadge;
        private JLabel lblRfid, lblTime, lblDur, lblFee, lblSocket;
        private CarCanvas carCanvas;

        public BayPanel(int id) {
            this.bayId = id;
            setLayout(new BorderLayout(8, 8));
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CARD_BORDER, 1),
                    new EmptyBorder(12, 14, 12, 14)
            ));

            // Header
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(CARD_BG);

            JLabel name = new JLabel("Ô ĐỖ SỐ " + bayId);
            name.setFont(FONT_HEADER);
            name.setForeground(TEXT_WHITE);

            lblStatusBadge = new JLabel("● ĐANG TRỐNG");
            lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblStatusBadge.setForeground(NEON_GREEN);

            top.add(name, BorderLayout.WEST);
            top.add(lblStatusBadge, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Center Visual
            carCanvas = new CarCanvas();
            carCanvas.setPreferredSize(new Dimension(280, 115));
            add(carCanvas, BorderLayout.CENTER);

            // Info panel
            JPanel info = new JPanel(new GridLayout(5, 1, 0, 5));
            info.setBackground(new Color(11, 15, 25));
            info.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 41, 59), 1),
                    new EmptyBorder(8, 12, 8, 12)
            ));

            lblRfid   = addRow(info, "Mã RFID:", "--");
            lblTime   = addRow(info, "Giờ vào:", "--");
            lblDur    = addRow(info, "Đang đỗ:", "--");
            lblFee    = addRow(info, "Cước tạm tính:", "--");
            lblSocket = addRow(info, "Trạm Socket:", "--");

            lblRfid.setFont(FONT_MONO);
            lblRfid.setForeground(NEON_CYAN);
            lblFee.setFont(FONT_MONO);
            lblFee.setForeground(NEON_GOLD);
            lblSocket.setFont(new Font("Consolas", Font.PLAIN, 11));
            lblSocket.setForeground(new Color(56, 189, 248));

            add(info, BorderLayout.SOUTH);
        }

        private JLabel addRow(JPanel parent, String label, String val) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(new Color(11, 15, 25));
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
                g2.drawRoundRect(12, 6, w - 24, h - 12, 10, 10);

                if (occupied) {
                    // Ve xe the thao Cyberpunk (dua xe len tren de khong bi che bien so)
                    int cx = w / 2 - 50;
                    int cy = h / 2 - 28;

                    // Den pha chieu sang
                    GradientPaint beam = new GradientPaint(cx + 90, cy + 15, new Color(254, 240, 138, 120),
                            cx + 140, cy + 15, new Color(6, 182, 212, 0));
                    g2.setPaint(beam);
                    g2.fillPolygon(new int[]{cx + 90, cx + 140, cx + 140, cx + 90},
                            new int[]{cy + 10, cy, cy + 30, cy + 20}, 4);

                    // Banh xe (dat tai cy + 22 den cy + 38)
                    g2.setColor(new Color(2, 6, 23));
                    g2.fillOval(cx + 10, cy + 22, 16, 16);
                    g2.fillOval(cx + 70, cy + 22, 16, 16);
                    g2.setColor(TEXT_MUTED);
                    g2.drawOval(cx + 10, cy + 22, 16, 16);
                    g2.drawOval(cx + 70, cy + 22, 16, 16);

                    // Than xe
                    Color carColor = (bayId == 1) ? new Color(56, 189, 248) :
                            (bayId == 2 ? new Color(244, 63, 94) : new Color(245, 158, 11));
                    g2.setColor(carColor);
                    g2.fillRoundRect(cx, cy + 8, 100, 20, 12, 12);

                    // Cabin & Kinh
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(cx + 25, cy - 4, 45, 16, 8, 8);
                    g2.setColor(NEON_CYAN);
                    g2.fillRoundRect(cx + 45, cy - 1, 20, 10, 4, 4);

                    // Den pha LED
                    g2.setColor(new Color(254, 240, 138));
                    g2.fillOval(cx + 96, cy + 12, 5, 8);

                    // Bien so the RFID (Dat ben duoi hanh trinh banh xe, KHONG BI DE LEN BANH XE)
                    int plateY = cy + 44;
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(cx + 8, plateY, 84, 18, 6, 6);
                    g2.setColor(new Color(15, 23, 42));
                    g2.drawRoundRect(cx + 8, plateY, 84, 18, 6, 6);
                    g2.setFont(new Font("Consolas", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = cx + 8 + (84 - fm.stringWidth(rfid)) / 2;
                    int textY = plateY + ((18 - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(rfid, textX, textY);
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
