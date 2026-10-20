const isoDate = (d) => new Date(d - d.getTimezoneOffset() * 6e4).toISOString().slice(0, 10);

export const today = () => isoDate(new Date());

/** 사진 파일의 수정일을 방문일 기본값으로 쓴다 */
export const fileDate = (f) => (f.lastModified ? isoDate(new Date(f.lastModified)) : today());

export const fmtDate = (s) => {
  if (!s) return "";
  const [y, m, d] = s.split("-");
  return `${y}. ${+m}. ${+d}.`;
};

/** 올리기 전에 긴 변을 1600px 로 줄여 JPEG 로 바꾼다 */
export async function shrink(file) {
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise((ok, no) => {
      const i = new Image();
      i.onload = () => ok(i);
      i.onerror = no;
      i.src = url;
    });
    const max = 1600, k = Math.min(1, max / Math.max(img.naturalWidth, img.naturalHeight));
    const c = document.createElement("canvas");
    c.width = Math.round(img.naturalWidth * k);
    c.height = Math.round(img.naturalHeight * k);
    c.getContext("2d").drawImage(img, 0, 0, c.width, c.height);
    return await new Promise((ok) => c.toBlob((b) => ok(b || file), "image/jpeg", 0.86));
  } catch {
    return file;
  } finally {
    URL.revokeObjectURL(url);
  }
}