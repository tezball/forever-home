export function SkeletonCard() {
  return (
    <div className="card animate-pulse">
      <div className="h-48 bg-gray-200 rounded-t-lg" />
      <div className="p-4 space-y-3">
        <div className="flex justify-between items-start">
          <div className="h-5 bg-gray-200 rounded w-24" />
          <div className="h-5 bg-gray-200 rounded-full w-20" />
        </div>
        <div className="h-4 bg-gray-200 rounded w-32" />
        <div className="space-y-2">
          <div className="h-3 bg-gray-200 rounded w-full" />
          <div className="h-3 bg-gray-200 rounded w-4/5" />
        </div>
      </div>
    </div>
  );
}
