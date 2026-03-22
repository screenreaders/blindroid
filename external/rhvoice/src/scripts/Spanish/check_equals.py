# Copyright (C) 2024-2026  Mateo Cedillo <angelitomateocedillo@gmail.com>

# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 2.1 of the License, or
# (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.

# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

import difflib
from sys import argv

def print_differences(file1, file2):
	with open(file1, "r") as f:
		text1 = f.readlines()
	with open(file2, "r") as f2:
		text2 = f2.readlines()
	assert len(text1) == len(text2), "No se puede hacer la diferencia porque ambos archivos no tienen el mismo número de líneas"
	for i, (line1, line2) in enumerate(zip(text1, text2), start=1):
		line1 = line1.strip()
		line2 = line2.strip()
		ratio = difflib.SequenceMatcher(None, line1, line2).ratio()
		if ratio < 1.0:
			print(f"Diferencia en la línea {i}, detalles a continuación:\nCopia: {line1}\nActual: {line2}\n")
	print("Listo, patrón.")

if __name__ == "__main__":
	print_differences(argv[1], argv[2])
