import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MusicPlayer extends JFrame {
    private Clip clip;
    private int currentTrack = 0;
    private boolean isPaused = false; // Pause state flag
    private boolean isStopped = false; // Stop state flag
    private String[] song = {"audio/addiction.wav", "audio/apt.wav", "audio/supersonic.wav", "audio/supernova.wav", "audio/fakeidol.wav", "audio/MyNameIs.wav", "audio/MaskOn.wav"};
    private String[] image = {"image/addiction.png", "image/apt.png", "image/supersonic.png", "image/supernova.jpg", "image/fakeidol.jpg", "image/MyNameIs.png", "image/MaskOn.png"};
    private String[] songTitles = {"고민중독", "Apt", "Supersonic", "Supernova", "가짜 아이돌", "My name is", "Mask On"};
    private JLabel albumLabel, timeLabel;
    private JSlider slider;
    private Timer timer; // Timer to update playback time
    private JList<String> playlist;

    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 사용자 지정 폰트 로드
        Font customFont = loadCustomFont("font/CookieRun.ttf", 14f);

        // Left Panel: Album image, time display, controls
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBackground(Color.WHITE);

        // Album image
        albumLabel = new JLabel(scaleImage(image[currentTrack], 300, 300)); // Album art scaled to 300x300
        playerPanel.add(albumLabel, BorderLayout.NORTH);

        // Time and slider panel
        JPanel sliderPanel = new JPanel(new BorderLayout());
        JLabel songTitleLabel = new JLabel(songTitles[currentTrack], JLabel.CENTER);
        songTitleLabel.setBackground(Color.WHITE);
        songTitleLabel.setOpaque(true); // Current song title display
        songTitleLabel.setFont(loadCustomFont("font/CookieRun.ttf", 18f));
        sliderPanel.add(songTitleLabel, BorderLayout.NORTH);
        timeLabel = new JLabel("00:00 / 00:00", JLabel.RIGHT); // Current time and total time display
        timeLabel.setOpaque(true);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setForeground(Color.BLACK);
        timeLabel.setFont(customFont); // 사용자 지정 폰트 적용

        slider = new JSlider(0, 100, 0); // slider with initial settings
        slider.setUI(new CustomSliderUI(slider));
        slider.setBackground(Color.WHITE);
        slider.setForeground(Color.BLACK);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setEnabled(false); // Initially disabled until a song loads

        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(timeLabel, BorderLayout.EAST);
        playerPanel.add(sliderPanel, BorderLayout.CENTER);

        // Control buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createButton("image/skip-back.png", e -> playPreviousTrack()));
        buttonPanel.add(createButton("image/play.png", e -> playMusic()));
        buttonPanel.add(createButton("image/pause.png", e -> pauseMusic()));
        buttonPanel.add(createButton("image/stop.png", e -> stopMusic()));
        buttonPanel.add(createButton("image/skip-forward.png", e -> playNextTrack()));

        playerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Right Panel: Playlist with enlarged font and track highlight
        playlist = new JList<>(songTitles);
        playlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlist.setSelectedIndex(currentTrack);
        playlist.setFont(customFont); // 사용자 지정 폰트 적용
        playlist.setFixedCellHeight(60);  // Set each cell height to 60 pixels
        playlist.setFixedCellWidth(200);  // Set each cell width to 200 pixels

        playlist.setCellRenderer(new CustomCellRenderer());

        JScrollPane playlistScrollPane = new JScrollPane(playlist);
        playlistScrollPane.setPreferredSize(new Dimension(200, 0)); // Half-width for playlist panel

        // Split Pane to divide left and right panels equally
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerPanel, playlistScrollPane);
        splitPane.setDividerLocation(300); // Set divider location for 50-50 split
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // Playlist event handling
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click to play selected track
                    currentTrack = playlist.getSelectedIndex();
                    playSelectedTrack();
                }
            }
        });

        // Add KeyListener for keyboard controls
        addKeyListener(new CustomKeyListener());
        setFocusable(true);
        requestFocusInWindow();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                requestFocusInWindow();
            }
        });

        setSize(600, 446); // Adjusted size for a balanced look
        setVisible(true);

        slider.addChangeListener(new SliderChangeListener());

        loadAudio(song[currentTrack]); // Initial song load without autoplay
    }

    private Font loadCustomFont(String path, float size) {
        try {
            InputStream is = new FileInputStream(path);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            return font.deriveFont(size);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int) size); // 기본 폰트로 대체
        }
    }

    private ImageIcon scaleImage(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    private JButton createButton(String imagePath, ActionListener actionListener) {
        JButton button = new JButton(scaleImage(imagePath, 26, 26));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(actionListener);
        return button;
    }

    private void loadAudio(String pathName) {
        try {
            File audioFile = new File(pathName);
            final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            clip = AudioSystem.getClip();
            clip.addLineListener(new ClipLineListener());
            clip.open(audioStream);
            slider.setEnabled(true);
            updateSliderAndTime();

            timer = new Timer(1000, e -> updateSliderAndTime());
            timer.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private void playMusic() {
        requestFocusInWindow();
        if (clip != null) {
            clip.start();
            isPaused = false; // Reset paused state
            isStopped = false; // Reset stopped state
            if (timer != null) timer.start(); // Start timer for playback updates
        }
    }

    private void updateSliderAndTime() {
        if (clip != null) {
            long currentFrame = clip.getFramePosition();
            long totalFrames = clip.getFrameLength();
            float frameRate = clip.getFormat().getFrameRate();
            long currentTimeInSeconds = (long) (currentFrame / frameRate);  // Cast to long
            long totalTimeInSeconds = (long) (totalFrames / frameRate);      // Cast to long

            slider.setValue((int) ((currentFrame * 100) / totalFrames));

            timeLabel.setText(String.format("%02d:%02d / %02d:%02d",
                    currentTimeInSeconds / 60, currentTimeInSeconds % 60,
                    totalTimeInSeconds / 60, totalTimeInSeconds % 60));
            playlist.repaint();
        }
    }

    private void pauseMusic() {
        if (clip != null && clip.isRunning()) {
            isPaused = true;
            clip.stop();
            if (timer != null) timer.stop();
        }
    }

    private void stopMusic() {
        if (clip != null) {
            isStopped = true;
            clip.stop();
            clip.setFramePosition(0);
            slider.setValue(0);
            if (timer != null) timer.stop();
        }
    }

    private void skipForward() {
        if (clip != null) {
            int newFrame = clip.getFramePosition() + (int) (clip.getFormat().getFrameRate() * 5);
            if (newFrame < clip.getFrameLength()) {
                clip.setFramePosition(newFrame);
            } else {
                playNextTrack();
            }
            updateSliderAndTime();
        }
    }

    private void skipBackward() {
        if (clip != null) {
            int newFrame = clip.getFramePosition() - (int) (clip.getFormat().getFrameRate() * 5);
            if (newFrame > 0) {
                clip.setFramePosition(newFrame);
            } else {
                clip.setFramePosition(0);
            }
            updateSliderAndTime();
        }
    }

    private void playPreviousTrack() {
        currentTrack = (currentTrack - 1 + song.length) % song.length;
        playSelectedTrack();
    }

    private void playNextTrack() {
        currentTrack = (currentTrack + 1) % song.length;
        playSelectedTrack();
    }

    private void playSelectedTrack() {
        requestFocusInWindow();
        stopMusic();
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        JLabel songTitleLabel = (JLabel) ((JPanel) slider.getParent()).getComponent(0); // Update song title label
        songTitleLabel.setText(songTitles[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        playlist.setSelectedIndex(currentTrack);
        playlist.repaint();
        playMusic();
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    // Custom Slider UI class
    private class CustomSliderUI extends BasicSliderUI {
        public CustomSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(240, 239, 239)); // track color
            g2d.fillRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(200, 199, 199)); // knob color
            g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        }
    }

    // Custom Cell Renderer for the playlist
    private class CustomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index == currentTrack) {
                renderer.setBackground(Color.LIGHT_GRAY); // Highlight currently playing track
            } else {
                renderer.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
            }
            renderer.setOpaque(true); // Ensure background color shows
            renderer.setPreferredSize(new Dimension(200, 40)); // Set width 200, height 40
            return renderer;
        }
    }

    // Custom Key Listener for keyboard controls
    private class CustomKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    if (clip != null) {
                        if (clip.isRunning()) pauseMusic();
                        else playMusic();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    skipForward();
                    break;
                case KeyEvent.VK_LEFT:
                    skipBackward();
                    break;
            }
        }
    }

    // Custom Line Listener for Clip events
    private class ClipLineListener implements LineListener {
        @Override
        public void update(LineEvent e) {
            if (e.getType() == LineEvent.Type.STOP && !isPaused && !isStopped) {
                playNextTrack();
            }
        }
    }

    // Custom Change Listener for Slider changes
    private class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (slider.getValueIsAdjusting() && clip != null) {
                int newFrame = (int) ((slider.getValue() / 100.0) * clip.getFrameLength());
                clip.setFramePosition(newFrame);
                updateSliderAndTime();
            }
        }
    }
}
