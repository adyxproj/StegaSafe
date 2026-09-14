package com.stegasafe.desktop;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.stegasafe.crypto.CryptoEngine;
import com.stegasafe.dto.CapacityInfo;
import com.stegasafe.dto.DecodeResult;
import com.stegasafe.exception.*;
import com.stegasafe.stego.LsbSteganographyService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * StegaSafe Windows Desktop Application
 * A modern Swing GUI for steganographic encoding and decoding.
 */
public class StegaSafeDesktopApp extends JFrame {

    // ── Core Engine (no Spring context needed: instantiated directly) ──────────
    private final CryptoEngine cryptoEngine = new CryptoEngine();
    private final LsbSteganographyService stegoService = new LsbSteganographyService(cryptoEngine);

    // ── State ─────────────────────────────────────────────────────────────────
    private File encodeImageFile;
    private File decodeImageFile;
    private byte[] lastEncodedBytes;
    private String lastEncodedFilename = "stegasafe_encoded.png";
    private boolean isDarkTheme = true;

    // ── Color Palette ─────────────────────────────────────────────────────────
    private static final Color CLR_BG_DARK     = new Color(0x0B, 0x0F, 0x19);
    private static final Color CLR_SURFACE_DARK = new Color(0x11, 0x18, 0x27);
    private static final Color CLR_CARD_DARK   = new Color(0x1A, 0x22, 0x34);
    private static final Color CLR_ACCENT      = new Color(0x06, 0xB6, 0xD4);  // cyan-500
    private static final Color CLR_ACCENT2     = new Color(0x8B, 0x5C, 0xF6);  // violet-500
    private static final Color CLR_GREEN       = new Color(0x10, 0xB9, 0x81);  // emerald-500
    private static final Color CLR_RED         = new Color(0xEF, 0x44, 0x44);  // red-500
    private static final Color CLR_AMBER       = new Color(0xF5, 0x9E, 0x0B);  // amber-500
    private static final Color CLR_TEXT_DARK   = new Color(0xF3, 0xF4, 0xF6);
    private static final Color CLR_MUTED_DARK  = new Color(0x94, 0xA3, 0xB8);
    private static final Color CLR_BORDER_DARK = new Color(0x1E, 0x40, 0x58);

    // ── Encode panel widgets ───────────────────────────────────────────────────
    private JLabel  encodeDropLabel, encodePreviewLabel, encodeDimLabel, encodeCapLabel;
    private JTextArea encodeMessageArea;
    private JCheckBox encodePassCheck;
    private JPasswordField encodePassField;
    private JProgressBar encodeCapMeter;
    private JLabel encodeCapText;
    private JButton encodeBtn, encodeDownloadBtn, encodeGenerateBtn;
    private JPanel encodeResultPanel;
    private JLabel encodeResultLabel;

    // ── Decode panel widgets ───────────────────────────────────────────────────
    private JLabel  decodeDropLabel, decodePreviewLabel;
    private JPasswordField decodePassField;
    private JButton decodeBtn;
    private JTextArea decodeOutputArea;
    private JLabel decodeBadgeLabel, decodeStatsLabel;
    private JButton decodeCopyBtn;
    private JPanel decodeResultPanel;

    // ── Capacity panel widgets ─────────────────────────────────────────────────
    private JLabel capDropLabel, capPreviewLabel;
    private JLabel capResLabel, capPixLabel, capRawLabel, capMaxLabel, capSizeLabel;
    private JPanel capResultPanel;

    // ─────────────────────────────────────────────────────────────────────────
    public StegaSafeDesktopApp() {
        applyTheme(isDarkTheme);
        setTitle("StegaSafe — Secure Image Steganography");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1060, 720);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        setIconImage(buildAppIcon());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(isDarkTheme ? CLR_BG_DARK : UIManager.getColor("Panel.background"));

        root.add(buildTopBar(),    BorderLayout.NORTH);
        root.add(buildTabbedPane(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   TOP BAR
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(isDarkTheme ? CLR_SURFACE_DARK : UIManager.getColor("MenuBar.background"));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER_DARK));
        bar.setPreferredSize(new Dimension(0, 56));

        // Brand
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        brand.setOpaque(false);
        JLabel icon = new JLabel("🛡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        JLabel title = new JLabel("StegaSafe");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(CLR_TEXT_DARK);
        JLabel sub = new JLabel("AES-256-GCM + LSB Steganography Engine");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(CLR_MUTED_DARK);
        brand.add(icon);
        brand.add(title);
        brand.add(sub);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 14));
        controls.setOpaque(false);

        JButton themeBtn = accentButton(isDarkTheme ? "☀ Light Mode" : "🌙 Dark Mode", CLR_CARD_DARK);
        themeBtn.addActionListener(e -> toggleTheme(themeBtn));

        JLabel versionBadge = badge("v1.0.0", CLR_ACCENT);
        controls.add(versionBadge);
        controls.add(themeBtn);

        bar.add(brand, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   TABBED PANE
    // ──────────────────────────────────────────────────────────────────────────
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("  🔒 Encode  ", buildEncodeTab());
        tabs.addTab("  🔓 Decode  ", buildDecodeTab());
        tabs.addTab("  📊 Capacity  ", buildCapacityTab());
        tabs.addTab("  ℹ About  ", buildAboutTab());
        return tabs;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   ENCODE TAB
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel buildEncodeTab() {
        JPanel tab = new JPanel(new BorderLayout(12, 12));
        tab.setBorder(new EmptyBorder(16, 16, 16, 16));
        tab.setOpaque(false);

        JPanel splitPane = new JPanel(new GridLayout(1, 2, 16, 0));
        splitPane.setOpaque(false);

        // ── Left: Image Upload ───────────────────────────────────────────────
        JPanel leftCard = card("📁  Carrier Image   (Step 1)");
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));

        encodeDropLabel = dropZoneLabel("Drop image here\nPNG, JPG, JPEG, BMP");
        encodeDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel dropWrap = new JPanel(new BorderLayout());
        dropWrap.setOpaque(false);
        dropWrap.add(encodeDropLabel, BorderLayout.CENTER);

        // Buttons row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setOpaque(false);
        JButton browseBtn = accentButton("📂 Browse", CLR_ACCENT);
        encodeGenerateBtn = accentButton("⚡ Generate Test", CLR_ACCENT2);
        btnRow.add(browseBtn);
        btnRow.add(encodeGenerateBtn);

        // Preview area
        encodePreviewLabel = new JLabel("", SwingConstants.CENTER);
        encodePreviewLabel.setPreferredSize(new Dimension(200, 160));
        encodePreviewLabel.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        encodePreviewLabel.setBackground(Color.BLACK);
        encodePreviewLabel.setOpaque(true);

        JPanel metaRow = new JPanel(new GridLayout(1, 2, 8, 0));
        metaRow.setOpaque(false);
        encodeDimLabel = metaLabel("Dimensions", "--×--");
        encodeCapLabel = metaLabel("Max Payload", "--");
        metaRow.add(encodeDimLabel);
        metaRow.add(encodeCapLabel);

        // Capacity bar
        encodeCapMeter = new JProgressBar(0, 100);
        encodeCapMeter.setValue(0);
        encodeCapMeter.setStringPainted(false);
        encodeCapMeter.setForeground(CLR_ACCENT);
        encodeCapMeter.setBackground(CLR_CARD_DARK);
        encodeCapMeter.setPreferredSize(new Dimension(0, 8));
        encodeCapMeter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));

        encodeCapText = new JLabel("0 / 0 bytes (0%)", SwingConstants.RIGHT);
        encodeCapText.setFont(new Font("Consolas", Font.PLAIN, 11));
        encodeCapText.setForeground(CLR_MUTED_DARK);

        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(dropWrap);
        leftCard.add(Box.createVerticalStrut(8));
        leftCard.add(btnRow);
        leftCard.add(Box.createVerticalStrut(10));
        leftCard.add(encodePreviewLabel);
        leftCard.add(Box.createVerticalStrut(8));
        leftCard.add(metaRow);
        leftCard.add(Box.createVerticalStrut(8));
        leftCard.add(encodeCapText);
        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(encodeCapMeter);
        leftCard.add(Box.createVerticalGlue());

        // ── Right: Message + Crypto ─────────────────────────────────────────
        JPanel rightCard = card("✏  Secret Message   (Step 2)");
        rightCard.setLayout(new BoxLayout(rightCard, BoxLayout.Y_AXIS));

        encodeMessageArea = new JTextArea(6, 30);
        encodeMessageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        encodeMessageArea.setLineWrap(true);
        encodeMessageArea.setWrapStyleWord(true);
        encodeMessageArea.setBackground(CLR_CARD_DARK);
        encodeMessageArea.setForeground(CLR_TEXT_DARK);
        encodeMessageArea.setCaretColor(CLR_ACCENT);
        encodeMessageArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        encodeMessageArea.setToolTipText("Type your confidential message here (supports Tamil, emojis, Unicode)");

        JScrollPane msgScroll = new JScrollPane(encodeMessageArea);
        msgScroll.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Presets row
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        presets.setOpaque(false);
        presets.setAlignmentX(Component.LEFT_ALIGNMENT);
        presets.add(new JLabel("Quick fill:"));
        for (String[] p : new String[][]{{"🏁 Flag", "flag"}, {"🇮🇳 Tamil", "tamil"}, {"🔑 API Key", "key"}}) {
            JButton pb = new JButton(p[0]);
            pb.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
            pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pb.setFocusPainted(false);
            String preset = p[1];
            pb.addActionListener(e -> {
                if ("flag".equals(preset)) encodeMessageArea.setText("FLAG{steg4s4fe_l5b_4es256_gcm_pr0t3ct10n_v1}");
                else if ("tamil".equals(preset)) encodeMessageArea.setText("வணக்கம் உலகம்! இது ஒரு பாதுகாப்பான ஸ்டெகனோகிராபி செய்தி. 🔐🚀");
                else encodeMessageArea.setText("SECRET_KEY=sk_live_abc123\nCLUSTER=prod-node-1\nPORT=8443");
                updateCapacityMeter();
            });
            presets.add(pb);
        }

        // Password toggle
        encodePassCheck = new JCheckBox("Enable AES-256-GCM Password Encryption");
        encodePassCheck.setFont(new Font("Segoe UI", Font.BOLD, 12));
        encodePassCheck.setForeground(CLR_TEXT_DARK);
        encodePassCheck.setOpaque(false);
        encodePassCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        encodePassCheck.addActionListener(e -> {
            encodePassField.setEnabled(encodePassCheck.isSelected());
            encodePassField.setBackground(encodePassCheck.isSelected() ? CLR_CARD_DARK : CLR_SURFACE_DARK);
            updateCapacityMeter();
        });

        encodePassField = new JPasswordField(30);
        encodePassField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        encodePassField.setBackground(CLR_SURFACE_DARK);
        encodePassField.setForeground(CLR_TEXT_DARK);
        encodePassField.setCaretColor(CLR_ACCENT);
        encodePassField.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(6, 8, 6, 8)));
        encodePassField.setEnabled(false);
        encodePassField.setAlignmentX(Component.LEFT_ALIGNMENT);
        encodePassField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        encodePassField.setToolTipText("Minimum 4 characters. Password is required to decrypt later.");

        // Encode button
        encodeBtn = primaryButton("🔒  Encode & Save PNG");
        encodeBtn.setEnabled(false);
        encodeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        encodeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Result
        encodeResultPanel = new JPanel(new BorderLayout(8, 0));
        encodeResultPanel.setOpaque(false);
        encodeResultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        encodeResultPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_GREEN),
                new EmptyBorder(8, 10, 8, 10)));
        encodeResultPanel.setVisible(false);

        encodeResultLabel = new JLabel("✓ Encoding successful! Click Download to save.");
        encodeResultLabel.setForeground(CLR_GREEN);
        encodeResultLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        encodeDownloadBtn = accentButton("⬇ Download PNG", CLR_GREEN);
        encodeResultPanel.add(encodeResultLabel, BorderLayout.CENTER);
        encodeResultPanel.add(encodeDownloadBtn, BorderLayout.EAST);

        rightCard.add(Box.createVerticalStrut(4));
        rightCard.add(msgScroll);
        rightCard.add(Box.createVerticalStrut(6));
        rightCard.add(presets);
        rightCard.add(Box.createVerticalStrut(12));
        rightCard.add(encodePassCheck);
        rightCard.add(Box.createVerticalStrut(6));
        rightCard.add(encodePassField);
        rightCard.add(Box.createVerticalStrut(14));
        rightCard.add(encodeBtn);
        rightCard.add(Box.createVerticalStrut(10));
        rightCard.add(encodeResultPanel);
        rightCard.add(Box.createVerticalGlue());

        splitPane.add(leftCard);
        splitPane.add(rightCard);

        tab.add(splitPane, BorderLayout.CENTER);

        // ── Wire up events ───────────────────────────────────────────────────
        setupEncodeDragDrop();

        browseBtn.addActionListener(e -> browseEncodeImage());
        encodeGenerateBtn.addActionListener(e -> generateSampleCarrier());
        encodeMessageArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateCapacityMeter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateCapacityMeter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCapacityMeter(); }
        });
        encodeBtn.addActionListener(e -> doEncode());
        encodeDownloadBtn.addActionListener(e -> downloadEncoded());

        return tab;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   DECODE TAB
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel buildDecodeTab() {
        JPanel tab = new JPanel(new BorderLayout(12, 12));
        tab.setBorder(new EmptyBorder(16, 16, 16, 16));
        tab.setOpaque(false);

        JPanel splitPane = new JPanel(new GridLayout(1, 2, 16, 0));
        splitPane.setOpaque(false);

        // ── Left: Image Upload ───────────────────────────────────────────────
        JPanel leftCard = card("🖼  Encoded Image");
        leftCard.setLayout(new BoxLayout(leftCard, BoxLayout.Y_AXIS));

        decodeDropLabel = dropZoneLabel("Drop encoded PNG here");
        decodeDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel dropWrap = new JPanel(new BorderLayout());
        dropWrap.setOpaque(false);
        dropWrap.add(decodeDropLabel, BorderLayout.CENTER);

        JPanel decBrowseRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        decBrowseRow.setOpaque(false);
        JButton decBrowseBtn = accentButton("📂 Browse PNG", CLR_ACCENT);
        decBrowseRow.add(decBrowseBtn);

        decodePreviewLabel = new JLabel("", SwingConstants.CENTER);
        decodePreviewLabel.setPreferredSize(new Dimension(200, 200));
        decodePreviewLabel.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        decodePreviewLabel.setBackground(Color.BLACK);
        decodePreviewLabel.setOpaque(true);

        leftCard.add(Box.createVerticalStrut(4));
        leftCard.add(dropWrap);
        leftCard.add(Box.createVerticalStrut(8));
        leftCard.add(decBrowseRow);
        leftCard.add(Box.createVerticalStrut(10));
        leftCard.add(decodePreviewLabel);
        leftCard.add(Box.createVerticalGlue());

        // ── Right: Password + Extract ────────────────────────────────────────
        JPanel rightCard = card("🔍  Extract Hidden Message");
        rightCard.setLayout(new BoxLayout(rightCard, BoxLayout.Y_AXIS));

        JLabel passLabel = new JLabel("Decryption Passphrase (leave empty if unencrypted)");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(CLR_TEXT_DARK);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        decodePassField = new JPasswordField(30);
        decodePassField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        decodePassField.setBackground(CLR_CARD_DARK);
        decodePassField.setForeground(CLR_TEXT_DARK);
        decodePassField.setCaretColor(CLR_ACCENT);
        decodePassField.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(6, 8, 6, 8)));
        decodePassField.setAlignmentX(Component.LEFT_ALIGNMENT);
        decodePassField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel passHint = new JLabel("AEAD authentication tag rejects wrong passwords instantly.");
        passHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        passHint.setForeground(CLR_MUTED_DARK);
        passHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        decodeBtn = primaryButton("🔓  Extract Secret Message");
        decodeBtn.setEnabled(false);
        decodeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        decodeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Output area
        decodeResultPanel = new JPanel();
        decodeResultPanel.setOpaque(false);
        decodeResultPanel.setLayout(new BoxLayout(decodeResultPanel, BoxLayout.Y_AXIS));
        decodeResultPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(10, 10, 10, 10)));
        decodeResultPanel.setVisible(false);
        decodeResultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgeRow.setOpaque(false);
        decodeBadgeLabel = new JLabel("DECRYPTED");
        decodeBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        decodeBadgeLabel.setForeground(CLR_GREEN);
        decodeBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_GREEN),
                new EmptyBorder(2, 6, 2, 6)));
        decodeStatsLabel = new JLabel();
        decodeStatsLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        decodeStatsLabel.setForeground(CLR_MUTED_DARK);
        decodeCopyBtn = accentButton("📋 Copy", CLR_ACCENT);
        decodeCopyBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        badgeRow.add(decodeBadgeLabel);
        badgeRow.add(decodeStatsLabel);
        badgeRow.add(Box.createHorizontalGlue());
        badgeRow.add(decodeCopyBtn);

        decodeOutputArea = new JTextArea(7, 30);
        decodeOutputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        decodeOutputArea.setLineWrap(true);
        decodeOutputArea.setWrapStyleWord(true);
        decodeOutputArea.setBackground(CLR_SURFACE_DARK);
        decodeOutputArea.setForeground(CLR_TEXT_DARK);
        decodeOutputArea.setEditable(false);

        JScrollPane decScroll = new JScrollPane(decodeOutputArea);
        decScroll.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        decScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        decodeResultPanel.add(badgeRow);
        decodeResultPanel.add(Box.createVerticalStrut(8));
        decodeResultPanel.add(decScroll);

        rightCard.add(Box.createVerticalStrut(4));
        rightCard.add(passLabel);
        rightCard.add(Box.createVerticalStrut(6));
        rightCard.add(decodePassField);
        rightCard.add(Box.createVerticalStrut(4));
        rightCard.add(passHint);
        rightCard.add(Box.createVerticalStrut(16));
        rightCard.add(decodeBtn);
        rightCard.add(Box.createVerticalStrut(12));
        rightCard.add(decodeResultPanel);
        rightCard.add(Box.createVerticalGlue());

        splitPane.add(leftCard);
        splitPane.add(rightCard);
        tab.add(splitPane, BorderLayout.CENTER);

        // ── Wire events ──────────────────────────────────────────────────────
        setupDecodeDragDrop();
        decBrowseBtn.addActionListener(e -> browseDecodeImage());
        decodeBtn.addActionListener(e -> doDecode());
        decodeCopyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(decodeOutputArea.getText()), null);
            showStatus("Message copied to clipboard!", CLR_GREEN);
        });

        return tab;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   CAPACITY TAB
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel buildCapacityTab() {
        JPanel tab = new JPanel(new BorderLayout(12, 12));
        tab.setBorder(new EmptyBorder(16, 16, 16, 16));
        tab.setOpaque(false);

        JPanel mainCard = card("📐  Image Steganography Capacity Inspector");
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));

        capDropLabel = dropZoneLabel("Drop any image to inspect capacity");
        capDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel capBrowseRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        capBrowseRow.setOpaque(false);
        JButton capBrowseBtn = accentButton("📂 Browse Image", CLR_ACCENT);
        capBrowseRow.add(capBrowseBtn);

        capPreviewLabel = new JLabel("", SwingConstants.CENTER);
        capPreviewLabel.setPreferredSize(new Dimension(200, 150));
        capPreviewLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        capPreviewLabel.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        capPreviewLabel.setBackground(Color.BLACK);
        capPreviewLabel.setOpaque(true);
        capPreviewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        capResultPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        capResultPanel.setOpaque(false);
        capResultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capResultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        capResultPanel.setVisible(false);

        capResLabel  = capStat("Resolution",       "--");
        capPixLabel  = capStat("Total Pixels",     "--");
        capRawLabel  = capStat("Raw Bit Capacity", "--");
        capMaxLabel  = capStat("Max Usable (AES)", "--");
        capSizeLabel = capStat("File Size",        "--");
        JLabel dummy = capStat("", "");

        capResultPanel.add(capResLabel);
        capResultPanel.add(capPixLabel);
        capResultPanel.add(capRawLabel);
        capResultPanel.add(capMaxLabel);
        capResultPanel.add(capSizeLabel);
        capResultPanel.add(dummy);

        mainCard.add(Box.createVerticalStrut(8));
        mainCard.add(capDropLabel);
        mainCard.add(Box.createVerticalStrut(8));
        mainCard.add(capBrowseRow);
        mainCard.add(Box.createVerticalStrut(12));
        mainCard.add(capPreviewLabel);
        mainCard.add(Box.createVerticalStrut(16));
        mainCard.add(capResultPanel);
        mainCard.add(Box.createVerticalGlue());

        tab.add(mainCard, BorderLayout.CENTER);

        setupCapacityDragDrop();
        capBrowseBtn.addActionListener(e -> {
            JFileChooser fc = imageChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loadCapacityFile(fc.getSelectedFile());
            }
        });
        return tab;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   ABOUT TAB
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel buildAboutTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBorder(new EmptyBorder(16, 16, 16, 16));
        tab.setOpaque(false);

        JTextArea about = new JTextArea();
        about.setEditable(false);
        about.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        about.setForeground(CLR_TEXT_DARK);
        about.setBackground(CLR_SURFACE_DARK);
        about.setLineWrap(true);
        about.setWrapStyleWord(true);
        about.setMargin(new Insets(16, 20, 16, 20));
        about.setText("""
                StegaSafe Desktop  —  v1.0.0
                ══════════════════════════════════════════════════════════════

                What is Image Steganography?
                ─────────────────────────────
                Steganography is the art of hiding secret information inside an innocent-
                looking carrier medium (such as a digital image) so that even the existence
                of the hidden message is concealed. Unlike cryptography, which makes data
                unreadable, steganography makes data invisible.

                How LSB (Least Significant Bit) Encoding Works
                ─────────────────────────────────────────────────
                Each pixel in a 24-bit image has three colour bytes: Red (R), Green (G),
                Blue (B). The least significant bit (LSB) of each byte has a visual weight
                of only 1/256, making a change completely imperceptible to the human eye.
                
                StegaSafe stores 3 bits per pixel: 1 bit in R, 1 bit in G, 1 bit in B.
                
                  pixel.R = (pixel.R & ~1) | secretBit0
                  pixel.G = (pixel.G & ~1) | secretBit1
                  pixel.B = (pixel.B & ~1) | secretBit2

                Why PNG Only for Export?
                ─────────────────────────
                JPEG uses lossy DCT compression that completely rewrites pixel values,
                destroying every LSB modification. PNG uses lossless DEFLATE compression,
                guaranteeing 100% bit-exact preservation. StegaSafe always exports PNG.

                Security Architecture
                ─────────────────────
                • Cipher:        AES-256 in Galois/Counter Mode (GCM) — AEAD
                • Authentication: 128-bit GCM authentication tag — detects tampering
                • Key Derivation: PBKDF2-HMAC-SHA256, 65,536 iterations, 16-byte salt
                • Salt & IV:     New cryptographic random values per encoding session
                • Zero Trace:    All operations run in RAM — nothing written to disk

                Binary Protocol (Packet Header inside Image)
                ─────────────────────────────────────────────
                  Bytes 0–3:    Magic header "STEG" (0x53 0x54 0x45 0x47)
                  Byte  4:      Protocol version (0x01)
                  Byte  5:      Flags (bit 0 = 1 if encrypted)
                  Bytes 6–21:   PBKDF2 salt (16 bytes, encrypted only)
                  Bytes 22–33:  AES-GCM IV (12 bytes, encrypted only)
                  Bytes 34–37:  Payload length (Big-Endian int32)
                  Bytes 38–N:   Ciphertext (with 16-byte GCM tag) or UTF-8 plaintext
                  Last 4:       CRC32 checksum (plaintext mode only)
                
                Built with: Java 21 • Spring Boot 3 • FlatLaf UI • AES-256-GCM
                """);

        JScrollPane scroll = new JScrollPane(about);
        scroll.setBorder(new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK));
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   ENCODE LOGIC
    // ──────────────────────────────────────────────────────────────────────────
    private void browseEncodeImage() {
        JFileChooser fc = imageChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadEncodeFile(fc.getSelectedFile());
        }
    }

    private void loadEncodeFile(File file) {
        encodeImageFile = file;
        encodeResultPanel.setVisible(false);
        loadPreview(file, encodePreviewLabel, 300, 200);
        try {
            CapacityInfo info = stegoService.analyzeCapacity(file);
            encodeDimLabel.setText("<html><b>" + info.getWidth() + " × " + info.getHeight() + " px</b></html>");
            encodeCapLabel.setText("<html><b style='color:#06B6D4'>" + info.getFormattedMaxCapacity() + "</b></html>");
            updateCapacityMeter();
            encodeBtn.setEnabled(!encodeMessageArea.getText().isBlank());
            showStatus("Carrier image loaded: " + file.getName(), CLR_ACCENT);
        } catch (Exception ex) {
            showError("Failed to inspect image: " + ex.getMessage());
        }
    }

    private void generateSampleCarrier() {
        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override protected File doInBackground() throws Exception {
                int w = 480, h = 320;
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x09, 0x0D, 0x16),
                        w, h, new Color(0x01, 0x69, 0xC7));
                g.setPaint(gp); g.fillRect(0, 0, w, h);
                g.setColor(new Color(56, 189, 248, 50));
                for (int x = 0; x < w; x += 20) { g.drawLine(x, 0, x, h); }
                for (int y = 0; y < h; y += 20) { g.drawLine(0, y, w, y); }
                g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g.drawString("STEGASAFE CARRIER", 100, h / 2 - 8);
                g.setColor(new Color(56, 189, 248)); g.setFont(new Font("Consolas", Font.PLAIN, 14));
                g.drawString("480 x 320 px  |  LSB Ready", 130, h / 2 + 22);
                g.dispose();
                File out = File.createTempFile("stegasafe_carrier_", ".png");
                ImageIO.write(img, "png", out);
                out.deleteOnExit();
                return out;
            }
            @Override protected void done() {
                try { loadEncodeFile(get()); showStatus("Sample carrier image generated (480×320)!", CLR_ACCENT); }
                catch (Exception ex) { showError("Could not generate image: " + ex.getMessage()); }
            }
        };
        worker.execute();
    }

    private void updateCapacityMeter() {
        if (encodeImageFile == null) return;
        try {
            String text = encodeMessageArea.getText();
            byte[] msgBytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            CapacityInfo info = stegoService.analyzeCapacity(encodeImageFile);
            boolean useEnc = encodePassCheck.isSelected();
            long maxBytes = useEnc ? info.getMaxMessageBytesEncrypted() : info.getMaxMessageBytesUnencrypted();
            if (maxBytes <= 0) return;
            int pct = (int) Math.min(100, (msgBytes.length * 100L / maxBytes));
            encodeCapMeter.setValue(pct);
            encodeCapMeter.setForeground(pct > 90 ? CLR_RED : pct > 60 ? CLR_AMBER : CLR_ACCENT);
            encodeCapText.setText(msgBytes.length + " / " + maxBytes + " bytes (" + pct + "%)");
            encodeBtn.setEnabled(!text.isBlank() && pct <= 100);
        } catch (Exception ignored) {}
    }

    private void doEncode() {
        if (encodeImageFile == null || encodeMessageArea.getText().isBlank()) return;
        String password = encodePassCheck.isSelected() ? new String(encodePassField.getPassword()) : null;
        if (encodePassCheck.isSelected() && (password == null || password.length() < 4)) {
            showError("Password must be at least 4 characters.");
            return;
        }
        encodeBtn.setText("⏳ Encoding...");
        encodeBtn.setEnabled(false);

        SwingWorker<byte[], Void> worker = new SwingWorker<>() {
            @Override protected byte[] doInBackground() throws Exception {
                return stegoService.encodeMessage(encodeImageFile, encodeMessageArea.getText(), password);
            }
            @Override protected void done() {
                encodeBtn.setText("🔒  Encode & Save PNG");
                encodeBtn.setEnabled(true);
                try {
                    lastEncodedBytes = get();
                    String base = encodeImageFile.getName().replaceFirst("\\.[^.]+$", "");
                    lastEncodedFilename = base + "_encoded.png";
                    encodeResultLabel.setText("✓ Encoding successful! " +
                            String.format("%.1f KB", lastEncodedBytes.length / 1024.0) +
                            (encodePassCheck.isSelected() ? " | AES-256-GCM Encrypted" : " | Plaintext LSB"));
                    encodeResultPanel.setVisible(true);
                    showStatus("Message embedded into PNG successfully!", CLR_GREEN);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showError(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void downloadEncoded() {
        if (lastEncodedBytes == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(lastEncodedFilename));
        fc.setFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith(".png")) {
                dest = new File(dest.getParentFile(), dest.getName() + ".png");
            }
            try {
                Files.write(dest.toPath(), lastEncodedBytes);
                showStatus("Saved encoded PNG to: " + dest.getAbsolutePath(), CLR_GREEN);
            } catch (IOException ex) {
                showError("Could not save file: " + ex.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   DECODE LOGIC
    // ──────────────────────────────────────────────────────────────────────────
    private void browseDecodeImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadDecodeFile(fc.getSelectedFile());
        }
    }

    private void loadDecodeFile(File file) {
        decodeImageFile = file;
        decodeResultPanel.setVisible(false);
        decodeOutputArea.setText("");
        loadPreview(file, decodePreviewLabel, 300, 220);
        decodeBtn.setEnabled(true);
        showStatus("Encoded image loaded: " + file.getName(), CLR_ACCENT);
    }

    private void doDecode() {
        if (decodeImageFile == null) return;
        String password = new String(decodePassField.getPassword());
        decodeBtn.setText("⏳ Extracting...");
        decodeBtn.setEnabled(false);

        SwingWorker<DecodeResult, Void> worker = new SwingWorker<>() {
            @Override protected DecodeResult doInBackground() throws Exception {
                return stegoService.decodeMessage(decodeImageFile, password.isEmpty() ? null : password);
            }
            @Override protected void done() {
                decodeBtn.setText("🔓  Extract Secret Message");
                decodeBtn.setEnabled(true);
                try {
                    DecodeResult result = get();
                    decodeOutputArea.setText(result.getMessage());
                    if (result.isEncrypted()) {
                        decodeBadgeLabel.setText("AES-256-GCM ENCRYPTED");
                        decodeBadgeLabel.setForeground(CLR_ACCENT2);
                        decodeBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                                new MatteBorder(1, 1, 1, 1, CLR_ACCENT2),
                                new EmptyBorder(2, 6, 2, 6)));
                    } else {
                        decodeBadgeLabel.setText("UNENCRYPTED LSB");
                        decodeBadgeLabel.setForeground(CLR_GREEN);
                        decodeBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                                new MatteBorder(1, 1, 1, 1, CLR_GREEN),
                                new EmptyBorder(2, 6, 2, 6)));
                    }
                    decodeStatsLabel.setText("  " + result.getCharacterCount() + " chars | " +
                            result.getPayloadSizeBytes() + " bytes | " +
                            result.getExtractionTimeMs() + "ms");
                    decodeResultPanel.setVisible(true);
                    showStatus("Secret message extracted successfully!", CLR_GREEN);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showError(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   CAPACITY LOGIC
    // ──────────────────────────────────────────────────────────────────────────
    private void loadCapacityFile(File file) {
        loadPreview(file, capPreviewLabel, 480, 180);
        try {
            CapacityInfo info = stegoService.analyzeCapacity(file);
            capResLabel.setText("<html><b style='color:#06B6D4'>" + info.getWidth() + " × " + info.getHeight() + " px</b><br><small>Resolution</small></html>");
            capPixLabel.setText("<html><b>" + String.format("%,d", info.getTotalPixels()) + "</b><br><small>Total Pixels</small></html>");
            capRawLabel.setText("<html><b>" + LsbSteganographyService.formatByteSize(info.getRawCapacityBytes()) + "</b><br><small>Raw Bit Capacity</small></html>");
            capMaxLabel.setText("<html><b style='color:#8B5CF6'>" + info.getFormattedMaxCapacity() + "</b><br><small>Max Usable (AES)</small></html>");
            capSizeLabel.setText("<html><b>" + info.getFormattedFileSize() + "</b><br><small>File Size</small></html>");
            capResultPanel.setVisible(true);
            showStatus("Capacity inspected: " + file.getName(), CLR_ACCENT);
        } catch (Exception ex) {
            showError("Failed to inspect capacity: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   STATUS BAR
    // ──────────────────────────────────────────────────────────────────────────
    private JLabel statusLabel;
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CLR_SURFACE_DARK);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, CLR_BORDER_DARK),
                new EmptyBorder(5, 14, 5, 14)));
        statusLabel = new JLabel("Ready  —  StegaSafe v1.0.0");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(CLR_MUTED_DARK);
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel java = new JLabel("Java " + System.getProperty("java.version") + "  •  FlatLaf");
        java.setFont(new Font("Consolas", Font.PLAIN, 11));
        java.setForeground(CLR_MUTED_DARK);
        bar.add(java, BorderLayout.EAST);
        return bar;
    }

    private void showStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(color);
        });
    }

    private void showError(String msg) {
        showStatus("⚠  " + msg, CLR_RED);
        JOptionPane.showMessageDialog(this, msg, "StegaSafe Error", JOptionPane.ERROR_MESSAGE);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   DRAG & DROP
    // ──────────────────────────────────────────────────────────────────────────
    private void setupEncodeDragDrop() {
        new DropTarget(encodeDropLabel, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                e.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadEncodeFile(files.get(0));
                } catch (Exception ignored) {}
            }
        });
    }

    private void setupDecodeDragDrop() {
        new DropTarget(decodeDropLabel, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                e.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadDecodeFile(files.get(0));
                } catch (Exception ignored) {}
            }
        });
    }

    private void setupCapacityDragDrop() {
        new DropTarget(capDropLabel, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                e.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadCapacityFile(files.get(0));
                } catch (Exception ignored) {}
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   UI HELPERS
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(CLR_SURFACE_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(14, 16, 14, 16)));

        TitledBorder tb = BorderFactory.createTitledBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK), "  " + title + "  ");
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        tb.setTitleColor(CLR_MUTED_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(10, 12, 10, 12)));
        return panel;
    }

    private JLabel dropZoneLabel(String text) {
        JLabel lbl = new JLabel("<html><center>🖼<br>" + text.replace("\n", "<br>") + "</center></html>",
                SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_CARD_DARK);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.setColor(CLR_BORDER_DARK);
                float[] dash = {6f, 4f};
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(CLR_MUTED_DARK);
        lbl.setPreferredSize(new Dimension(260, 100));
        lbl.setOpaque(false);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return lbl;
    }

    private JLabel metaLabel(String title, String value) {
        JLabel lbl = new JLabel("<html><span style='font-size:9px;color:#64748B;'>" + title + "</span><br><b>" + value + "</b></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(CLR_TEXT_DARK);
        lbl.setBackground(CLR_CARD_DARK);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(6, 8, 6, 8)));
        return lbl;
    }

    private JLabel capStat(String label, String value) {
        JLabel lbl = new JLabel("<html><div style='font-size:9px;color:#64748B;'>" + label + "</div><b>" + value + "</b></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(CLR_TEXT_DARK);
        lbl.setBackground(CLR_CARD_DARK);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, CLR_BORDER_DARK),
                new EmptyBorder(8, 12, 8, 12)));
        return lbl;
    }

    private JButton primaryButton(String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, CLR_ACCENT, getWidth(), 0, new Color(0x02, 0x84, 0xC7));
                if (!isEnabled()) g2.setColor(new Color(0x3B, 0x45, 0x56));
                else g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        return btn;
    }

    private JButton accentButton(String label, Color color) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btn.setForeground(color);
        btn.setBackground(CLR_CARD_DARK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, color),
                new EmptyBorder(5, 12, 5, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel badge(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(color);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, color),
                new EmptyBorder(2, 6, 2, 6)));
        return lbl;
    }

    private void loadPreview(File file, JLabel target, int maxW, int maxH) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                int iw = img.getWidth(), ih = img.getHeight();
                double scale = Math.min((double) maxW / iw, (double) maxH / ih);
                int nw = (int) (iw * scale), nh = (int) (ih * scale);
                Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
                target.setIcon(new ImageIcon(scaled));
                target.setText("");
            }
        } catch (Exception ignored) {}
    }

    private JFileChooser imageChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Image Files (PNG, JPG, JPEG, BMP)", "png", "jpg", "jpeg", "bmp"));
        return fc;
    }

    private BufferedImage buildAppIcon() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, CLR_ACCENT, 32, 32, CLR_ACCENT2);
        g.setPaint(gp);
        g.fillRoundRect(0, 0, 32, 32, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g.drawString("S", 9, 23);
        g.dispose();
        return img;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   THEME
    // ──────────────────────────────────────────────────────────────────────────
    private void applyTheme(boolean dark) {
        try {
            if (dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }
    }

    private void toggleTheme(JButton themeBtn) {
        isDarkTheme = !isDarkTheme;
        applyTheme(isDarkTheme);
        SwingUtilities.updateComponentTreeUI(this);
        themeBtn.setText(isDarkTheme ? "☀ Light Mode" : "🌙 Dark Mode");
        showStatus("Switched to " + (isDarkTheme ? "dark" : "light") + " theme.", CLR_ACCENT);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //   ENTRY POINT
    // ──────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Ensure proper HiDPI scaling on Windows
        System.setProperty("sun.java2d.uiScale", "1.0");
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(StegaSafeDesktopApp::new);
    }
}
