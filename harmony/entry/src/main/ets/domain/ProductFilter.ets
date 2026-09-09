import type { Product } from '../model/Models';

export function filterProducts(products: Product[], query: string, category: string): Product[] {
  const normalizedQuery = query.trim().toLocaleLowerCase();
  const normalizedCategory = category.trim();
  const categoryExists = products.some((product: Product) => product.category === normalizedCategory);
  return products.filter((product: Product) => {
    const matchesQuery = normalizedQuery.length === 0 ||
      product.name.toLocaleLowerCase().includes(normalizedQuery) ||
      product.barcode.toLocaleLowerCase().includes(normalizedQuery);
    const matchesCategory = normalizedCategory.length === 0 || normalizedCategory === '全部' || !categoryExists ||
      product.category === normalizedCategory;
    return matchesQuery && matchesCategory;
  });
}
