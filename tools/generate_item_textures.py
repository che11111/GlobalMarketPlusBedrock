"""Generate texture paths only from a vanilla Java client archive (no bundled artwork).

Usage: python3 tools/generate_item_textures.py /path/to/client.jar [version]
Bundled source: Java 26.3, SHA1 e877b6a07acd633fb3bb475002175cec036e7b87.
Understands pre-1.21.4 models, modern item definition trees and 26.x sprite objects.
Special renderers/tints cannot be reproduced by a static form image.
"""
import json
import sys
import zipfile
from pathlib import Path


def generate(archive):
    with zipfile.ZipFile(archive) as client:
        def definitions(prefix):
            return {name[len(prefix):-5]: json.loads(client.read(name))
                    for name in client.namelist() if name.startswith(prefix) and name.endswith('.json')}
        models = definitions('assets/minecraft/models/')
        items = definitions('assets/minecraft/items/')
        prefix = 'assets/minecraft/textures/'
        available = {name[len(prefix):-4] for name in client.namelist()
                     if name.startswith(prefix) and name.endswith('.png')}

    def texture(name):
        values = {}
        for _ in range(32):
            model = models.get(name, {})
            for key, value in model.get('textures', {}).items():
                values.setdefault(key, value)
            name = model.get('parent', '').removeprefix('minecraft:')
            if name not in models:
                break
        for key in ('layer0', 'all', 'particle', 'side', 'front', 'top', 'end', 'texture'):
            value = values.get(key, '')
            if isinstance(value, dict):
                # 26.x model files can attach display metadata to the sprite reference.
                value = value.get('sprite', '')
            if not isinstance(value, str):
                continue
            for _ in range(32):
                if not value.startswith('#'):
                    break
                value = values.get(value[1:], '')
            value = value.removeprefix('minecraft:')
            if value in available:
                return value

    def candidates(node):
        if not isinstance(node, dict):
            return
        for key in ('model', 'base'):
            if isinstance(node.get(key), str):
                yield node[key].removeprefix('minecraft:')
        # Prefer the GUI branch for display_context (e.g. trident), otherwise defaults.
        cases = node.get('cases', [])
        gui = [case for case in cases if 'gui' in case.get('when', [])]
        for case in gui:
            yield from candidates(case.get('model'))
        for key in ('fallback', 'on_false', 'on_true', 'model'):
            yield from candidates(node.get(key))
        for key in ('models', 'entries', 'cases'):
            for child in node.get(key, []):
                yield from candidates(child)

    result = {}
    if items:
        for item, definition in sorted(items.items()):
            for candidate in candidates(definition):
                value = texture(candidate)
                if value:
                    result[item.upper()] = value
                    break
    else:
        for name in sorted(models):
            if name.startswith('item/'):
                value = texture(name)
                if value:
                    result[name[5:].upper()] = value
    # Bukkit names can change across the supported 1.16+ server range.
    for old, current in {'GRASS': 'SHORT_GRASS', 'SCUTE': 'TURTLE_SCUTE', 'CHAIN': 'IRON_CHAIN'}.items():
        if current in result:
            result.setdefault(old, result[current])
    return result


if __name__ == '__main__':
    mapping = generate(sys.argv[1])
    version = sys.argv[2] if len(sys.argv) > 2 else '26.3'
    output = Path(__file__).resolve().parents[1] / 'src/main/resources/item-textures.yml'
    output.write_text(f'# Generated from Minecraft Java {version} item definitions. Texture paths only.\n'
                      + ''.join(f"{key}: '{value}'\n" for key, value in sorted(mapping.items())), encoding='utf-8')
    print(f'Wrote {len(mapping)} texture paths to {output}')
