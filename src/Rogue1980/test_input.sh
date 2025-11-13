#!/bin/bash
#!/bin/bash
echo "=== Тест терминала ==="
echo "TERM=$TERM"
echo "В вас в TTY: $(tty)"
echo "Текущий режим:"
stty -g

echo -e "\n=== Тест ввода (нажимайте клавиши, Enter для выхода) ==="
stty raw -echo
while true; do
    read -r -n 1 key
    printf "\nКод: %d | Символ: '%s'\n" "'$key" "$key"
    [ "$key" == $'\x0D' ] && break  # Enter
done
stty sane
EOF
