// 角色文案统一映射：user=旅客，attendant=乘务员，admin=车站管理员，super_admin=超级管理员
export const ROLE_TEXTS = {
  user: "旅客",
  attendant: "乘务员",
  admin: "车站管理员",
  super_admin: "超级管理员",
};

// 不再用“未知角色”兜底，未匹配时返回空串
export function roleTextOf(role) {
  return ROLE_TEXTS[role] || "";
}
