{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    # Java 21 for Minecraft 1.21.1
    jdk21

    # Gradle
    gradle

    # Useful tools
    git
    jq
  ];

  shellHook = ''
    echo "=== Gestorage Dev Environment ==="
    echo "Minecraft: 1.21.1"
    echo "Java: $(java -version 2>&1 | head -1)"
    echo "Gradle: $(gradle --version | head -3 | tail -1)"
    echo ""
    echo "Run 'gradle build' to build the mod"
    echo "Run 'gradle runClient' to test in-game"
    echo "=================================="
  '';

  # Ensure Java 21 is used
  JAVA_HOME = "${pkgs.jdk21}";
}
