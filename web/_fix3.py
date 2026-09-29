#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""executeOk 返回 boolean：const { ok } = await executeOk(...) -> const ok = await executeOk(...)（一次性脚本）"""
import io
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

for f in ['src/views/care/ElderView.vue', 'src/views/care/CareEventView.vue',
          'src/views/care/MedicationView.vue', 'src/views/care/CareDeviceView.vue']:
    s = io.open(f, encoding='utf-8').read()
    s = s.replace('const { ok } = await executeOk(', 'const ok = await executeOk(')
    s = s.replace('const { execute, loading: handleLoading } = useRequest()',
                  'const { executeOk, loading: handleLoading } = useRequest()')
    io.open(f, 'w', encoding='utf-8', newline='').write(s)
    print('fixed', f)
